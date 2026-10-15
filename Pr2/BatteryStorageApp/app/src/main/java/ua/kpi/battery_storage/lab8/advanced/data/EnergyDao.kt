package ua.kpi.battery_storage.lab8.advanced.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Режим роботи для фільтра записів (передається в SQL як рядок). */
enum class ModeFilter(val title: String) {
    ALL("Усі"),
    CHARGING("Заряд"),
    DISCHARGING("Розряд"),
    IDLE("Очікування"),
}

/** Порядок сортування записів (передається в SQL як рядок). */
enum class RecordSort(val title: String) {
    TIME_DESC("Новіші"),
    SOC_DESC("SoC ↓"),
    TEMP_DESC("t° ↓"),
    POWER_DESC("|P| ↓"),
}

/**
 * DAO у вигляді абстрактного класу: так можна додати @Transaction-метод з власною логікою,
 * що викликає кілька інших запитів атомарно.
 */
@Dao
abstract class EnergyDao {

    @Query("SELECT COUNT(*) FROM sites")
    abstract suspend fun countSites(): Int

    @Query("SELECT * FROM storage_units")
    abstract suspend fun getUnits(): List<StorageUnitEntity>

    @Query("SELECT stateOfCharge FROM energy_records WHERE unitId = :unitId ORDER BY recordedAt DESC LIMIT 1")
    abstract suspend fun latestSoc(unitId: Long): Double?

    @Query("SELECT MAX(recordedAt) FROM energy_records")
    abstract suspend fun latestRecordTime(): Long?

    @Insert
    abstract suspend fun insertSite(site: SiteEntity): Long

    @Insert
    abstract suspend fun insertUnit(unit: StorageUnitEntity): Long

    @Insert
    abstract suspend fun insertRecords(records: List<EnergyRecordEntity>)

    @Query("DELETE FROM energy_records WHERE recordedAt < :before")
    abstract suspend fun deleteRecordsBefore(before: Long): Int

    /** Майданчики з установками та записами (використовує дворівневу @Relation). */
    @Transaction
    @Query("SELECT * FROM sites ORDER BY name")
    abstract fun observeSitesWithUnits(): Flow<List<SiteWithUnits>>

    /**
     * Створює майданчик разом з установками в одній транзакції:
     * або зберігаються всі рядки, або (при помилці) жоден.
     * @return ідентифікатори створених установок
     */
    @Transaction
    open suspend fun insertSiteWithUnits(site: SiteEntity, units: List<StorageUnitEntity>): List<Long> {
        val siteId = insertSite(site)
        return units.map { insertUnit(it.copy(siteId = siteId)) }
    }

    /**
     * Обчислювані поля: агрегація записів по кожній установці.
     * latestSoc — корельований підзапит останнього значення SoC.
     * :siteId = NULL означає «усі майданчики».
     */
    @Query(
        """
        SELECT u.id AS unitId, u.name AS unitName, s.name AS siteName,
               u.capacityKwh AS capacityKwh, u.maxPowerKw AS maxPowerKw,
               COUNT(r.id) AS recordCount,
               (SELECT r2.stateOfCharge FROM energy_records r2
                 WHERE r2.unitId = u.id ORDER BY r2.recordedAt DESC LIMIT 1) AS latestSoc,
               COALESCE(AVG(r.stateOfCharge), 0) AS avgSoc,
               COALESCE(MAX(r.temperatureC), 0) AS maxTemperature,
               COALESCE(SUM(CASE WHEN r.powerKw > 0 THEN r.powerKw * r.intervalHours ELSE 0 END), 0) AS chargedKwh,
               COALESCE(SUM(CASE WHEN r.powerKw < 0 THEN -r.powerKw * r.intervalHours ELSE 0 END), 0) AS dischargedKwh
        FROM storage_units u
        JOIN sites s ON s.id = u.siteId
        LEFT JOIN energy_records r ON r.unitId = u.id
        WHERE (:siteId IS NULL OR u.siteId = :siteId)
        GROUP BY u.id
        ORDER BY s.name, u.name
        """,
    )
    abstract fun observeUnitSummaries(siteId: Long?): Flow<List<UnitSummary>>

    /**
     * Фільтрація (майданчик, установка, режим) і сортування виконуються в SQL,
     * тож у пам'ять потрапляють лише потрібні рядки.
     */
    @Query(
        """
        SELECT r.*, u.name AS unitName
        FROM energy_records r
        JOIN storage_units u ON u.id = r.unitId
        WHERE (:siteId IS NULL OR u.siteId = :siteId)
          AND (:unitId IS NULL OR r.unitId = :unitId)
          AND (:mode = 'ALL'
               OR (:mode = 'CHARGING' AND r.powerKw > 0)
               OR (:mode = 'DISCHARGING' AND r.powerKw < 0)
               OR (:mode = 'IDLE' AND r.powerKw = 0))
        ORDER BY
          CASE WHEN :sort = 'SOC_DESC' THEN r.stateOfCharge END DESC,
          CASE WHEN :sort = 'TEMP_DESC' THEN r.temperatureC END DESC,
          CASE WHEN :sort = 'POWER_DESC' THEN ABS(r.powerKw) END DESC,
          r.recordedAt DESC
        LIMIT :limit
        """,
    )
    abstract fun observeRecords(
        siteId: Long?,
        unitId: Long?,
        mode: String,
        sort: String,
        limit: Int = 150,
    ): Flow<List<RecordWithUnit>>
}
