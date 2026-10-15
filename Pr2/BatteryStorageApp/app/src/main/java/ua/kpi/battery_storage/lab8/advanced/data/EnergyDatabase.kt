package ua.kpi.battery_storage.lab8.advanced.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

@Database(
    entities = [SiteEntity::class, StorageUnitEntity::class, EnergyRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class EnergyDatabase : RoomDatabase() {
    abstract fun energyDao(): EnergyDao

    companion object {
        @Volatile
        private var instance: EnergyDatabase? = null

        fun getInstance(context: Context): EnergyDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, EnergyDatabase::class.java, "advanced_energy.db")
                .build()
                .also { instance = it }
        }
    }
}

/**
 * Віддалене джерело (імітація API диспетчерського центру): генерує пакет телеметрії
 * для кожної установки. Запит триває [latencyMillis] і з імовірністю [failureRate] падає.
 */
class RemoteEnergySource(
    private val random: Random = Random.Default,
    private val failureRate: Double = 0.25,
    private val latencyMillis: Long = 1_200,
) {
    /** Нові записи для кожної установки, що продовжують її ряд від [startSoc]. */
    suspend fun fetchTelemetry(
        units: List<StorageUnitEntity>,
        from: Long,
        points: Int,
        startSoc: (StorageUnitEntity) -> Double,
    ): List<EnergyRecordEntity> {
        delay(latencyMillis)
        if (random.nextDouble() < failureRate) throw IOException("Сервер диспетчерського центру недоступний (HTTP 503)")
        return units.flatMap { unit -> generateSeries(unit, from, points, startSoc(unit)) }
    }

    /** Ряд записів з кроком 15 хв: SoC змінюється відповідно до потужності. */
    fun generateSeries(unit: StorageUnitEntity, from: Long, points: Int, startSoc: Double = 50.0): List<EnergyRecordEntity> {
        var soc = startSoc
        return List(points) { i ->
            val power = when (random.nextInt(4)) {
                0 -> 0.0
                1, 2 -> (random.nextDouble(0.2, 0.9) * unit.maxPowerKw).roundToInt().toDouble()
                else -> -(random.nextDouble(0.2, 0.9) * unit.maxPowerKw).roundToInt().toDouble()
            }.let { p -> if ((soc >= 98 && p > 0) || (soc <= 5 && p < 0)) -p else p }
            soc = (soc + power * 0.25 / unit.capacityKwh * 100).coerceIn(0.0, 100.0)
            EnergyRecordEntity(
                unitId = unit.id,
                recordedAt = from + i * 15 * 60_000L,
                stateOfCharge = (soc * 10).roundToInt() / 10.0,
                powerKw = power,
                temperatureC = ((24 + abs(power) / unit.maxPowerKw * 12 + random.nextDouble(-1.5, 1.5)) * 10).roundToInt() / 10.0,
            )
        }
    }
}
