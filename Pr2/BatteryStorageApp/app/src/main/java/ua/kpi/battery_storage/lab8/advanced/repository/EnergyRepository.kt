package ua.kpi.battery_storage.lab8.advanced.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ua.kpi.battery_storage.lab8.advanced.data.EnergyDao
import ua.kpi.battery_storage.lab8.advanced.data.ModeFilter
import ua.kpi.battery_storage.lab8.advanced.data.RecordSort
import ua.kpi.battery_storage.lab8.advanced.data.RecordWithUnit
import ua.kpi.battery_storage.lab8.advanced.data.RemoteEnergySource
import ua.kpi.battery_storage.lab8.advanced.data.SiteEntity
import ua.kpi.battery_storage.lab8.advanced.data.StorageUnitEntity
import ua.kpi.battery_storage.lab8.advanced.data.UnitSummary

/** Результат операції репозиторію: успіх або помилка з поясненням. */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Error(val message: String) : DataResult<Nothing>
}

/** Коротка інформація про майданчик для фільтра. */
data class SiteOverview(val site: SiteEntity, val unitCount: Int, val recordCount: Int)

/** Звідки отримано дані під час синхронізації. */
enum class SyncSource(val title: String) { CACHE("кеш (дані свіжі)"), REMOTE("віддалений сервер") }

/** Агреговані показники парку накопичувачів. */
data class FleetStatistics(
    val unitCount: Int,
    val totalCapacityKwh: Double,
    val totalStoredKwh: Double,
    val chargedKwh: Double,
    val dischargedKwh: Double,
    val hottestUnit: UnitSummary?,
) {
    val fleetSoc: Double get() = if (totalCapacityKwh > 0) totalStoredKwh / totalCapacityKwh * 100 else 0.0
    val roundTripEfficiency: Double? get() = if (chargedKwh > 0) dischargedKwh / chargedKwh else null
}

/** Бізнес-логіка агрегації: зважений SoC, сумарна енергія, найгарячіша установка. */
object FleetAnalytics {
    fun aggregate(units: List<UnitSummary>): FleetStatistics = FleetStatistics(
        unitCount = units.size,
        totalCapacityKwh = units.sumOf { it.capacityKwh },
        totalStoredKwh = units.sumOf { it.storedKwh },
        chargedKwh = units.sumOf { it.chargedKwh },
        dischargedKwh = units.sumOf { it.dischargedKwh },
        hottestUnit = units.maxByOrNull { it.maxTemperature },
    )
}

/**
 * Repository просунутого рівня. Агрегує два джерела (Room і RemoteEnergySource):
 *  - Room — єдине джерело істини для UI (offline-first): екрани спостерігають Flow з бази;
 *  - віддалені дані після завантаження кешуються в Room;
 *  - повторний запит у межах [cacheTtlMillis] обслуговується з кешу без звернення до мережі;
 *  - мережеві помилки перехоплюються, запит повторюється ([maxAttempts] спроб) і
 *    повертається DataResult.Error замість винятку;
 *  - Mutex не дозволяє запустити дві синхронізації одночасно.
 */
class EnergyRepository(
    private val dao: EnergyDao,
    private val remote: RemoteEnergySource,
    private val cacheTtlMillis: Long = 30_000,
    private val maxAttempts: Int = 2,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val syncMutex = Mutex()
    private var lastSyncAt: Long? = null

    fun observeSites(): Flow<List<SiteOverview>> = dao.observeSitesWithUnits().map { sites ->
        sites.map { s -> SiteOverview(s.site, s.units.size, s.units.sumOf { it.records.size }) }
    }

    fun observeUnitSummaries(siteId: Long?): Flow<List<UnitSummary>> = dao.observeUnitSummaries(siteId)

    /** Статистика обчислюється з тих самих агрегатів, тому завжди узгоджена зі списком. */
    fun observeFleetStatistics(siteId: Long?): Flow<FleetStatistics> =
        dao.observeUnitSummaries(siteId).map(FleetAnalytics::aggregate)

    fun observeRecords(siteId: Long?, unitId: Long?, mode: ModeFilter, sort: RecordSort): Flow<List<RecordWithUnit>> =
        dao.observeRecords(siteId, unitId, mode.name, sort.name)

    /** Перший запуск: створює демонстраційні майданчики й історію за останні 12 годин. */
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.countSites() > 0) return@withContext
        val start = clock() - 12 * 60 * 60_000L
        val kyivUnits = dao.insertSiteWithUnits(
            SiteEntity(name = "Київ-Північ", region = "Київська обл."),
            listOf(
                StorageUnitEntity(siteId = 0, name = "BESS-K1", capacityKwh = 500.0, maxPowerKw = 250.0),
                StorageUnitEntity(siteId = 0, name = "BESS-K2", capacityKwh = 250.0, maxPowerKw = 125.0),
            ),
        )
        val lvivUnits = dao.insertSiteWithUnits(
            SiteEntity(name = "Львів-Захід", region = "Львівська обл."),
            listOf(StorageUnitEntity(siteId = 0, name = "BESS-L1", capacityKwh = 1000.0, maxPowerKw = 500.0)),
        )
        val units = dao.getUnits().filter { it.id in kyivUnits + lvivUnits }
        dao.insertRecords(units.flatMap { remote.generateSeries(it, start, points = 40, startSoc = 40.0) })
    }

    /**
     * Синхронізація з віддаленим сервером з урахуванням кешу.
     * @param force ігнорувати TTL кешу
     */
    suspend fun sync(force: Boolean): DataResult<SyncSource> = syncMutex.withLock {
        val last = lastSyncAt
        if (!force && last != null && clock() - last < cacheTtlMillis) {
            return@withLock DataResult.Success(SyncSource.CACHE)
        }
        var lastError: String? = null
        repeat(maxAttempts) { attempt ->
            try {
                val units = dao.getUnits()
                // Новий пакет продовжує часовий ряд з останнього збереженого запису
                val from = (dao.latestRecordTime()?.plus(STEP_MS) ?: clock())
                val latestSoc = units.associate { it.id to (dao.latestSoc(it.id) ?: 50.0) }
                val records = remote.fetchTelemetry(units, from, points = 4) { latestSoc.getValue(it.id) }
                dao.insertRecords(records)
                lastSyncAt = clock()
                return@withLock DataResult.Success(SyncSource.REMOTE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e.message
                if (attempt < maxAttempts - 1) delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        DataResult.Error("Не вдалося синхронізувати після $maxAttempts спроб: $lastError")
    }

    /** Очищує записи, старші за [hours] годин; повертає кількість видалених. */
    suspend fun purgeOlderThan(hours: Int): Int = dao.deleteRecordsBefore(clock() - hours * 60 * 60_000L)

    private companion object {
        const val RETRY_DELAY_MS = 500L
        const val STEP_MS = 15 * 60_000L
    }
}
