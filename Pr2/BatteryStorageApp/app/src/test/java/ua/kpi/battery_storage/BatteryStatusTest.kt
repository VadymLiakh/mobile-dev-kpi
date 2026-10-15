package ua.kpi.battery_storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.battery_storage.model.BatteryMode
import ua.kpi.battery_storage.model.BatteryStatus
import kotlin.random.Random

class BatteryStatusTest {

    @Test
    fun storedEnergy_isProportionalToStateOfCharge() {
        val status = BatteryStatus(stateOfCharge = 40, powerKw = 0, temperatureC = 25.0)
        assertEquals(200.0, status.storedEnergyKwh, 1e-9)
    }

    @Test
    fun mode_dependsOnPowerSign() {
        assertEquals(BatteryMode.CHARGING, BatteryStatus(50, 10, 25.0).mode)
        assertEquals(BatteryMode.DISCHARGING, BatteryStatus(50, -10, 25.0).mode)
        assertEquals(BatteryMode.IDLE, BatteryStatus(50, 0, 25.0).mode)
    }

    @Test
    fun random_staysWithinPhysicalLimits() {
        val random = Random(42)
        repeat(1_000) {
            val s = BatteryStatus.random(random)
            assertTrue(s.stateOfCharge in 0..100)
            assertTrue(s.powerKw in -BatteryStatus.MAX_POWER_KW..BatteryStatus.MAX_POWER_KW)
            assertTrue(s.temperatureC in 20.0..38.0)
            // Повна батарея не заряджається, порожня — не розряджається
            if (s.stateOfCharge == 100) assertTrue(s.powerKw <= 0)
            if (s.stateOfCharge == 0) assertTrue(s.powerKw >= 0)
        }
    }
}
