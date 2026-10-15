package ua.kpi.battery_storage.lab6

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.battery_storage.lab6.domain.BatteryChemistry
import ua.kpi.battery_storage.lab6.domain.BatteryFormValidator
import ua.kpi.battery_storage.lab6.domain.FormRules
import ua.kpi.battery_storage.lab6.domain.OperatorRegistry
import ua.kpi.battery_storage.lab6.domain.PasswordStrength

class FormValidationTest {

    @Test
    fun email_format() {
        assertNull(FormRules.email("operator@bess.ua"))
        assertNull(FormRules.email("v.liakh@kpi.edu.ua"))
        assertNotNull(FormRules.email(""))
        assertNotNull(FormRules.email("operator@"))
        assertNotNull(FormRules.email("operator.bess.ua"))
        assertNotNull(FormRules.email("a@b.c"))
    }

    @Test
    fun password_minLength() {
        assertNotNull(FormRules.minPasswordLength("short"))
        assertNull(FormRules.minPasswordLength("longenough"))
    }

    @Test
    fun numberInRange_acceptsCommaDecimal() {
        assertNull(FormRules.numberInRange("12,5", "X", 10.0..20.0, "од."))
        assertNotNull(FormRules.numberInRange("25", "X", 10.0..20.0, "од."))
        assertNotNull(FormRules.numberInRange("abc", "X", 10.0..20.0, "од."))
    }

    @Test
    fun passwordStrength_levels() {
        assertEquals(PasswordStrength.WEAK, BatteryFormValidator.passwordStrength("abc"))
        assertEquals(PasswordStrength.MEDIUM, BatteryFormValidator.passwordStrength("Battery10"))
        assertEquals(PasswordStrength.STRONG, BatteryFormValidator.passwordStrength("Battery#10"))
        assertNull(BatteryFormValidator.validatePassword("Battery#10"))
        assertNotNull(BatteryFormValidator.validatePassword("Battery10"))
    }

    @Test
    fun power_isLimitedByChemistryCRate() {
        // LFP 1C: 500 кВт·год → максимум 500 кВт
        assertNull(BatteryFormValidator.validatePower("500", "500", BatteryChemistry.LFP))
        assertNotNull(BatteryFormValidator.validatePower("600", "500", BatteryChemistry.LFP))
        // NMC 2C: 500 кВт·год → максимум 1000 кВт
        assertNull(BatteryFormValidator.validatePower("600", "500", BatteryChemistry.NMC))
        assertNotNull(BatteryFormValidator.validatePower("0", "500", BatteryChemistry.NMC))
    }

    @Test
    fun registry_detectsTakenEmailIgnoringCase() = runTest {
        val registry = OperatorRegistry(setOf("admin@bess.ua"))
        assertTrue(registry.isEmailTaken("Admin@BESS.ua"))
        assertFalse(registry.isEmailTaken("new@bess.ua"))
        registry.register("new@bess.ua")
        assertTrue(registry.isEmailTaken("new@bess.ua"))
    }
}
