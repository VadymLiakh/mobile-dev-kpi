package ua.kpi.battery_storage.lab7.medium

import ua.kpi.battery_storage.model.BatteryMode
import ua.kpi.battery_storage.ui.components.formatDecimals
import kotlin.math.abs
import kotlin.math.min

/** Поля форми середнього рівня ПЗ 7. */
enum class CalcField { CAPACITY, SOC, POWER }

/** Похідні величини, обчислені з введених параметрів. */
data class BatteryCalculation(
    val capacityKwh: Double,
    val stateOfCharge: Double,
    val powerKw: Double,
) {
    val storedKwh: Double get() = capacityKwh * stateOfCharge / 100.0
    val freeKwh: Double get() = capacityKwh - storedKwh

    val mode: BatteryMode
        get() = when {
            powerKw > 0 -> BatteryMode.CHARGING
            powerKw < 0 -> BatteryMode.DISCHARGING
            else -> BatteryMode.IDLE
        }

    /** C-rate: відношення потужності до ємності, 1/год. */
    val cRate: Double get() = abs(powerKw) / capacityKwh

    /** Час до повного заряду (при заряджанні) або до повного розряду (при розряджанні), год. */
    val hoursToLimit: Double?
        get() = when (mode) {
            BatteryMode.CHARGING -> freeKwh / powerKw
            BatteryMode.DISCHARGING -> storedKwh / abs(powerKw)
            BatteryMode.IDLE -> null
        }

    /** Прогноз рівня заряду погодинно, доки батарея не досягне межі (не більше 12 год). */
    fun hourlyProjection(maxHours: Int = 12): List<Pair<Int, Double>> {
        val deltaPerHour = powerKw / capacityKwh * 100.0
        val result = mutableListOf(0 to stateOfCharge)
        var soc = stateOfCharge
        for (hour in 1..maxHours) {
            if (mode == BatteryMode.IDLE || soc <= 0.0 || soc >= 100.0) break
            soc = min(100.0, (soc + deltaPerHour).coerceAtLeast(0.0))
            result += hour to soc
        }
        return result
    }

    val warnings: List<String>
        get() = buildList {
            if (cRate > 1.0) add("C-rate ${cRate.formatDecimals(2)} перевищує 1C — прискорене старіння батареї")
            if (stateOfCharge < 10 && mode == BatteryMode.DISCHARGING) add("Глибокий розряд: заряд нижче 10 %")
            if (stateOfCharge > 95 && mode == BatteryMode.CHARGING) add("Батарея майже повна — знизьте потужність")
        }
}

/**
 * Бізнес-логіка середнього рівня: перевірка введених рядків і побудова розрахунку.
 * Не залежить від Android, тому легко тестується.
 */
object BatteryCalculator {
    fun validate(capacity: String, soc: String, power: String): Map<CalcField, String> = buildMap {
        val c = capacity.toDoubleOrNull()
        when {
            c == null -> put(CalcField.CAPACITY, "Введіть число")
            c <= 0 -> put(CalcField.CAPACITY, "Ємність має бути > 0")
        }
        val s = soc.toDoubleOrNull()
        when {
            s == null -> put(CalcField.SOC, "Введіть число")
            s !in 0.0..100.0 -> put(CalcField.SOC, "Діапазон 0–100 %")
        }
        val p = power.toDoubleOrNull()
        when {
            p == null -> put(CalcField.POWER, "Введіть число (− розряд)")
            c != null && c > 0 && abs(p) > c * 3 -> put(CalcField.POWER, "Не більше 3C (${(c * 3).toInt()} кВт)")
        }
    }

    fun calculate(capacity: String, soc: String, power: String): BatteryCalculation? =
        if (validate(capacity, soc, power).isNotEmpty()) {
            null
        } else {
            BatteryCalculation(capacity.toDouble(), soc.toDouble(), power.toDouble())
        }
}
