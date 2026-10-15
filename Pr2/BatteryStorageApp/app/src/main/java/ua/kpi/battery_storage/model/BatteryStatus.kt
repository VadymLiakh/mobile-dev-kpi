package ua.kpi.battery_storage.model

import kotlin.random.Random

/** Режим роботи системи накопичення енергії. */
enum class BatteryMode(val title: String) {
    CHARGING("Заряджання"),
    DISCHARGING("Розряджання"),
    IDLE("Очікування"),
}

/**
 * Поточний стан системи накопичення енергії (Battery Energy Storage System).
 *
 * @param stateOfCharge рівень заряду (SoC), %
 * @param powerKw потужність обміну з мережею, кВт: «+» — заряджання, «−» — розряджання
 * @param temperatureC температура батарейних модулів, °C
 */
data class BatteryStatus(
    val stateOfCharge: Int,
    val powerKw: Int,
    val temperatureC: Double,
) {
    /** Енергія, накопичена на цей момент, кВт·год. */
    val storedEnergyKwh: Double
        get() = CAPACITY_KWH * stateOfCharge / 100.0

    val mode: BatteryMode
        get() = when {
            powerKw > 0 -> BatteryMode.CHARGING
            powerKw < 0 -> BatteryMode.DISCHARGING
            else -> BatteryMode.IDLE
        }

    companion object {
        /** Номінальна ємність накопичувача, кВт·год. */
        const val CAPACITY_KWH = 500

        /** Максимальна потужність інвертора, кВт. */
        const val MAX_POWER_KW = 250

        val Initial = BatteryStatus(stateOfCharge = 50, powerKw = 0, temperatureC = 25.0)

        /**
         * Симулює нове показання телеметрії. Повністю заряджена батарея не заряджається,
         * а повністю розряджена — не віддає енергію.
         */
        fun random(random: Random = Random.Default): BatteryStatus {
            val soc = random.nextInt(0, 101)
            val power = when (soc) {
                100 -> random.nextInt(-MAX_POWER_KW, 1)
                0 -> random.nextInt(0, MAX_POWER_KW + 1)
                else -> random.nextInt(-MAX_POWER_KW, MAX_POWER_KW + 1)
            }
            val temperature = 20.0 + random.nextDouble() * 18.0
            return BatteryStatus(soc, power, temperature)
        }
    }
}
