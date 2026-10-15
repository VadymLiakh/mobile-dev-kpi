package ua.kpi.battery_storage.lab8.medium

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Віддалене джерело даних (імітація REST API сервера SCADA).
 * Кожен запит триває близько секунди — значно довше, ніж читання з Room.
 */
class RemoteTelemetryApi(private val random: Random = Random.Default) {

    /** Каталог модулів, зареєстрованих на сервері. */
    suspend fun fetchModuleCatalog(): List<BatteryModuleEntity> {
        delay(LATENCY_MS)
        return listOf(
            BatteryModuleEntity(name = "Стійка A-1", capacityKwh = 125.0),
            BatteryModuleEntity(name = "Стійка A-2", capacityKwh = 125.0),
            BatteryModuleEntity(name = "Стійка B-1", capacityKwh = 250.0),
        )
    }

    /** Останні показання для переданих модулів. */
    suspend fun fetchLatestReadings(modules: List<BatteryModuleEntity>, now: Long): List<ModuleReadingEntity> {
        delay(LATENCY_MS)
        return modules.map {
            ModuleReadingEntity(
                moduleId = it.id,
                stateOfCharge = random.nextInt(10, 100),
                temperatureC = 22.0 + random.nextDouble() * 14.0,
                recordedAt = now,
                source = SOURCE_REMOTE,
            )
        }
    }

    companion object {
        const val LATENCY_MS = 1_000L
        const val SOURCE_REMOTE = "remote"
        const val SOURCE_LOCAL = "local"
    }
}

/** Звідки репозиторій узяв дані під час останньої операції. */
enum class DataOrigin(val title: String) {
    LOCAL_CACHE("локальна база Room"),
    REMOTE("віддалений сервер"),
}

/**
 * Repository об'єднує два джерела: Room (швидке, працює офлайн) та RemoteTelemetryApi.
 * Правило вибору: UI завжди читає з Room; до сервера звертаємося лише якщо база порожня
 * або локальні дані застаріли (старші за [freshnessMillis]). Отримані з сервера дані
 * зберігаються в Room, і Flow автоматично оновлює UI.
 */
class MediumRepository(
    private val dao: ModuleDao,
    private val remote: RemoteTelemetryApi,
    private val freshnessMillis: Long = 60_000,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var lastSyncAt: Long? = null

    private val _lastOrigin = MutableStateFlow<DataOrigin?>(null)
    val lastOrigin: StateFlow<DataOrigin?> = _lastOrigin.asStateFlow()

    fun observeModules(query: String): Flow<List<ModuleWithReadings>> = dao.searchModules(query.trim())

    /**
     * Синхронізація з вибором оптимального джерела.
     * @param force true — примусово звернутися до сервера, навіть якщо кеш свіжий
     */
    suspend fun sync(force: Boolean = false): DataOrigin {
        var modules = dao.getModules()
        val sync = lastSyncAt
        val fresh = sync != null && clock() - sync < freshnessMillis
        if (modules.isNotEmpty() && fresh && !force) {
            return DataOrigin.LOCAL_CACHE.also { _lastOrigin.value = it }
        }
        if (modules.isEmpty()) {
            dao.insertModules(remote.fetchModuleCatalog())
            modules = dao.getModules()
        }
        dao.insertReadings(remote.fetchLatestReadings(modules, clock()))
        lastSyncAt = clock()
        return DataOrigin.REMOTE.also { _lastOrigin.value = it }
    }

    /** Додає модуль; повертає текст помилки, якщо назва вже зайнята. */
    suspend fun addModule(name: String, capacityKwh: Double): String? = try {
        dao.insertModule(BatteryModuleEntity(name = name.trim(), capacityKwh = capacityKwh))
        null
    } catch (e: SQLiteConstraintException) {
        "Модуль «${name.trim()}» уже існує"
    }

    suspend fun updateModule(module: BatteryModuleEntity): String? = try {
        dao.updateModule(module.copy(name = module.name.trim()))
        null
    } catch (e: SQLiteConstraintException) {
        "Модуль «${module.name.trim()}» уже існує"
    }

    suspend fun deleteModule(module: BatteryModuleEntity) = dao.deleteModule(module)

    suspend fun addLocalReading(moduleId: Long, stateOfCharge: Int, temperatureC: Double) {
        dao.insertReadings(
            listOf(
                ModuleReadingEntity(
                    moduleId = moduleId,
                    stateOfCharge = stateOfCharge,
                    temperatureC = temperatureC,
                    recordedAt = clock(),
                    source = RemoteTelemetryApi.SOURCE_LOCAL,
                ),
            ),
        )
    }
}
