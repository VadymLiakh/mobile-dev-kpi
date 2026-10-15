package ua.kpi.battery_storage.lab5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.battery_storage.lab5.advanced.BatteryControlState
import ua.kpi.battery_storage.lab5.advanced.ChargeSettings
import ua.kpi.battery_storage.lab5.advanced.OperatorProfile
import ua.kpi.battery_storage.lab5.advanced.OperatorRole
import ua.kpi.battery_storage.lab5.home.AuthValidator
import kotlin.random.Random

class Lab5LogicTest {

    @Test
    fun routes_areBuiltFromArguments() {
        assertEquals("medium_status/42/-80/90", Screen.Medium.Status.createRoute(42, -80, 90))
        assertEquals("advanced_module/3", Screen.Advanced.Module.createRoute(3))
        assertEquals(Screen.Advanced.Module, Screen.advancedByRoute(Screen.Advanced.Module.route))
        assertEquals(Screen.Advanced.Menu, Screen.advancedByRoute(null))
    }

    @Test
    fun login_validation() {
        assertNotNull(AuthValidator.validateLogin(""))
        assertNotNull(AuthValidator.validateLogin("abc"))
        assertNotNull(AuthValidator.validateLogin("оператор"))
        assertNull(AuthValidator.validateLogin("operator_1"))
    }

    @Test
    fun password_mustContainDigit() {
        assertNotNull(AuthValidator.validatePassword("batteryx"))
        assertNull(AuthValidator.validatePassword("battery10"))
    }

    @Test
    fun registration_rejectsTakenLoginAndMismatch() {
        val users = mapOf("operator" to "battery10")
        assertNotNull(AuthValidator.validateRegistration(users, "operator", "secret12", "secret12"))
        assertNotNull(AuthValidator.validateRegistration(users, "newuser", "secret12", "secret13"))
        assertNull(AuthValidator.validateRegistration(users, "newuser", "secret12", "secret12"))
    }

    @Test
    fun authenticate_checksUserAndPassword() {
        val users = mapOf("operator" to "battery10")
        assertNull(AuthValidator.authenticate(users, "operator", "battery10"))
        assertNotNull(AuthValidator.authenticate(users, "operator", "wrong1"))
        assertNotNull(AuthValidator.authenticate(users, "ghost", "battery10"))
    }

    @Test
    fun chargeSettings_requireMinimalWindow() {
        assertNotNull(ChargeSettings.validate(40, 50))
        assertNotNull(ChargeSettings.validate(2, 90))
        assertNull(ChargeSettings.validate(20, 80))
    }

    @Test
    fun observer_cannotChangeSettings() {
        val state = BatteryControlState(Random(1))
        state.updateProfile(OperatorProfile(role = OperatorRole.OBSERVER))
        assertFalse(state.canEditSettings)
        assertNotNull(state.applySettings(20, 80))

        state.updateProfile(OperatorProfile(role = OperatorRole.ENGINEER, onDuty = true))
        assertTrue(state.canEditSettings)
        assertNull(state.applySettings(20, 80))
        assertEquals(ChargeSettings(20, 80), state.settings)
    }

    @Test
    fun alerts_reportModulesOutsideRange() {
        val state = BatteryControlState(Random(7))
        state.applySettings(45, 70)
        val outside = state.modules.count { it.soc !in 45..70 }
        val socAlerts = state.alerts.count { "заряд" in it.text }
        assertEquals(outside, socAlerts)
    }
}
