package ua.kpi.battery_storage.lab7.advanced.domain

import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.math.abs
import kotlin.random.Random

/**
 * MODEL (шар бізнес-логіки MVVM). Тут описано дані та правила предметної області;
 * цей код нічого не знає про Compose і ViewModel.
 */

/** Одне показання телеметрії накопичувача. */
data class TelemetryReading(
    val timestampMillis: Long,
    val stateOfCharge: Double,
    val powerKw: Double,
    val temperatureC: Double,
    val voltageV: Double,
)

/** Джерело телеметрії. Інтерфейс дозволяє підмінити симулятор реальним API або тестовим фейком. */
interface TelemetrySource {
    /** Асинхронно отримує нове показання; може кинути IOException при збої зв'язку. */
    suspend fun fetch(): TelemetryReading
}

/**
 * Симулятор контролера BMS: кожне наступне показання є «випадковим блуканням» від попереднього,
 * запит триває [latencyMillis], а з імовірністю [failureRate] завершується помилкою зв'язку.
 */
class SimulatedTelemetrySource(
    private val random: Random = Random.Default,
    private val failureRate: Double = 0.2,
    private val latencyMillis: Long = 900,
    private val clock: () -> Long = System::currentTimeMillis,
) : TelemetrySource {
    private var last = TelemetryReading(clock(), 55.0, 80.0, 27.0, 760.0)

    override suspend fun fetch(): TelemetryReading {
        delay(latencyMillis)
        if (random.nextDouble() < failureRate) throw IOException("Контролер BMS не відповідає")

        val power = (last.powerKw + random.nextDouble(-60.0, 60.0)).coerceIn(-250.0, 250.0)
        // 5 хвилин роботи з потужністю power для батареї 500 кВт·год
        val soc = (last.stateOfCharge + power / 500.0 * 100.0 / 12.0).coerceIn(0.0, 100.0)
        val temperature = (last.temperatureC + abs(power) / 250.0 - 0.6 + random.nextDouble(-0.4, 0.4))
            .coerceIn(18.0, 45.0)
        val voltage = 700.0 + soc * 1.2
        last = TelemetryReading(clock(), soc, power, temperature, voltage)
        return last
    }
}

/** Загальний стан системи за результатами аналізу. */
enum class HealthStatus(val title: String) {
    NORMAL("Норма"),
    WARNING("Увага"),
    CRITICAL("Критично"),
}

/** Тенденція зміни рівня заряду. */
enum class SocTrend(val title: String) { RISING("зростає ↗"), FALLING("спадає ↘"), STABLE("стабільний →") }

/** Висновки аналізатора, які відображає UI. */
data class BatteryInsights(
    val status: HealthStatus,
    val trend: SocTrend,
    val averageSoc: Double,
    val peakTemperatureC: Double,
    val alerts: List<String>,
)

/** Бізнес-логіка аналізу історії показань. */
object BatteryAnalyzer {
    const val TEMP_WARNING = 35.0
    const val TEMP_CRITICAL = 40.0

    fun analyze(history: List<TelemetryReading>): BatteryInsights {
        require(history.isNotEmpty()) { "Історія показань порожня" }
        val latest = history.last()
        val alerts = buildList {
            if (latest.temperatureC >= TEMP_CRITICAL) add("Перегрів модулів: потрібне аварійне охолодження")
            else if (latest.temperatureC >= TEMP_WARNING) add("Температура вище норми — обмежте потужність")
            if (latest.stateOfCharge <= 10) add("Низький заряд: зарезервуйте енергію для аварійних потреб")
            if (latest.stateOfCharge >= 95 && latest.powerKw > 0) add("Батарея майже заряджена — припиніть заряджання")
        }
        val status = when {
            latest.temperatureC >= TEMP_CRITICAL || latest.stateOfCharge <= 5 -> HealthStatus.CRITICAL
            alerts.isNotEmpty() -> HealthStatus.WARNING
            else -> HealthStatus.NORMAL
        }
        val delta = if (history.size >= 2) latest.stateOfCharge - history[history.size - 2].stateOfCharge else 0.0
        val trend = when {
            delta > 0.5 -> SocTrend.RISING
            delta < -0.5 -> SocTrend.FALLING
            else -> SocTrend.STABLE
        }
        return BatteryInsights(
            status = status,
            trend = trend,
            averageSoc = history.map { it.stateOfCharge }.average(),
            peakTemperatureC = history.maxOf { it.temperatureC },
            alerts = alerts,
        )
    }
}
