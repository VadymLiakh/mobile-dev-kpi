package ua.kpi.battery_storage.lab8.advanced.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Схема бази просунутого рівня — три таблиці з двома зв'язками «один-до-багатьох»:
 *   sites (майданчики) 1 ──< storage_units (установки) 1 ──< energy_records (записи телеметрії)
 */

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val region: String,
)

@Entity(
    tableName = "storage_units",
    foreignKeys = [
        ForeignKey(SiteEntity::class, ["id"], ["siteId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("siteId")],
)
data class StorageUnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val siteId: Long,
    val name: String,
    val capacityKwh: Double,
    val maxPowerKw: Double,
)

/** Запис телеметрії за інтервал [intervalHours]: знак powerKw задає напрям (+ заряд, − розряд). */
@Entity(
    tableName = "energy_records",
    foreignKeys = [
        ForeignKey(StorageUnitEntity::class, ["id"], ["unitId"], onDelete = ForeignKey.CASCADE),
    ],
    // Складений індекс прискорює вибірку останніх записів установки
    indices = [Index("unitId", "recordedAt")],
)
data class EnergyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val recordedAt: Long,
    val stateOfCharge: Double,
    val powerKw: Double,
    val temperatureC: Double,
    val intervalHours: Double = 0.25,
)

/** Вкладений зв'язок: установка з усіма записами. */
data class UnitWithRecords(
    @Embedded val unit: StorageUnitEntity,
    @Relation(parentColumn = "id", entityColumn = "unitId")
    val records: List<EnergyRecordEntity>,
)

/** Майданчик з установками (кожна — з записами): дворівнева @Relation. */
data class SiteWithUnits(
    @Embedded val site: SiteEntity,
    @Relation(entity = StorageUnitEntity::class, parentColumn = "id", entityColumn = "siteId")
    val units: List<UnitWithRecords>,
)

/** Запис разом з назвою установки (результат JOIN). */
data class RecordWithUnit(
    @Embedded val record: EnergyRecordEntity,
    @ColumnInfo(name = "unitName") val unitName: String,
)

/**
 * Агреговані показники установки, обчислені SQL-запитом (GROUP BY + SUM/AVG/MAX).
 * Похідні величини (ККД, еквівалентні цикли) обчислюються властивостями класу.
 */
data class UnitSummary(
    val unitId: Long,
    val unitName: String,
    val siteName: String,
    val capacityKwh: Double,
    val maxPowerKw: Double,
    val recordCount: Int,
    val latestSoc: Double?,
    val avgSoc: Double,
    val maxTemperature: Double,
    val chargedKwh: Double,
    val dischargedKwh: Double,
) {
    /** Накопичена зараз енергія, кВт·год. */
    val storedKwh: Double get() = capacityKwh * (latestSoc ?: 0.0) / 100.0

    /** Ефективність циклу (round-trip efficiency): віддана енергія / прийнята. */
    val roundTripEfficiency: Double? get() = if (chargedKwh > 0) dischargedKwh / chargedKwh else null

    /** Кількість еквівалентних повних циклів розряду. */
    val equivalentCycles: Double get() = dischargedKwh / capacityKwh
}
