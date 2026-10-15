package ua.kpi.battery_storage.lab7.home

import ua.kpi.battery_storage.model.BatteryMode

/**
 * MODEL домашнього завдання ПЗ 7: чиста бізнес-логіка зміни рівня заряду.
 * Враховує ККД перетворення: при заряджанні в батарею потрапляє лише частина енергії,
 * при розряджанні з батареї витрачається більше, ніж віддається в мережу.
 */
object BatterySimulator {
    const val CAPACITY_KWH = 500.0
    const val EFFICIENCY = 0.95

    /**
     * Новий рівень заряду після роботи протягом [hours] з потужністю [powerKw] у режимі [mode].
     * Результат обмежено діапазоном [minSoc]..[maxSoc].
     */
    fun step(
        soc: Double,
        mode: BatteryMode,
        powerKw: Double,
        hours: Double,
        minSoc: Double = 0.0,
        maxSoc: Double = 100.0,
    ): Double {
        val energyKwh = when (mode) {
            BatteryMode.CHARGING -> powerKw * hours * EFFICIENCY
            BatteryMode.DISCHARGING -> -powerKw * hours / EFFICIENCY
            BatteryMode.IDLE -> 0.0
        }
        return (soc + energyKwh / CAPACITY_KWH * 100.0).coerceIn(minSoc, maxSoc)
    }

    /** Чи досягнуто межі, на якій симуляцію слід зупинити. */
    fun limitReached(soc: Double, mode: BatteryMode): Boolean = when (mode) {
        BatteryMode.CHARGING -> soc >= 100.0
        BatteryMode.DISCHARGING -> soc <= 0.0
        BatteryMode.IDLE -> false
    }
}
