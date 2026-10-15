package ua.kpi.battery_storage.lab7

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.kpi.battery_storage.lab7.advanced.domain.BatteryAnalyzer
import ua.kpi.battery_storage.lab7.advanced.domain.HealthStatus
import ua.kpi.battery_storage.lab7.advanced.domain.SocTrend
import ua.kpi.battery_storage.lab7.advanced.domain.TelemetryReading
import ua.kpi.battery_storage.lab7.advanced.domain.TelemetrySource
import ua.kpi.battery_storage.lab7.advanced.viewmodel.TelemetryUiState
import ua.kpi.battery_storage.lab7.advanced.viewmodel.TelemetryViewModel
import ua.kpi.battery_storage.lab7.home.BatterySimulator
import ua.kpi.battery_storage.lab7.medium.BatteryCalculator
import ua.kpi.battery_storage.lab7.medium.CalcField
import ua.kpi.battery_storage.lab7.medium.MediumBatteryViewModel
import ua.kpi.battery_storage.model.BatteryMode
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class Lab7LogicTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun calculator_derivesEnergyAndTime() {
        val calc = requireNotNull(BatteryCalculator.calculate("500", "60", "100"))
        assertEquals(300.0, calc.storedKwh, 1e-9)
        assertEquals(200.0, calc.freeKwh, 1e-9)
        assertEquals(BatteryMode.CHARGING, calc.mode)
        assertEquals(2.0, calc.hoursToLimit!!, 1e-9)
        assertEquals(0.2, calc.cRate, 1e-9)
    }

    @Test
    fun calculator_validation() {
        val errors = BatteryCalculator.validate("0", "120", "x")
        assertEquals(setOf(CalcField.CAPACITY, CalcField.SOC, CalcField.POWER), errors.keys)
        assertNull(BatteryCalculator.calculate("500", "120", "10"))
    }

    @Test
    fun projection_stopsAtFullCharge() {
        val calc = requireNotNull(BatteryCalculator.calculate("100", "80", "50"))
        val projection = calc.hourlyProjection()
        assertEquals(100.0, projection.last().second, 1e-9)
        assertTrue(projection.size <= 3)
    }

    @Test
    fun mediumViewModel_recalculatesOnInput() = runTest(dispatcher) {
        val vm = MediumBatteryViewModel()
        vm.onSocChange("20")
        vm.onPowerChange("-50")
        advanceUntilIdle()
        val calc = requireNotNull(vm.calculation.value)
        assertEquals(BatteryMode.DISCHARGING, calc.mode)
        assertEquals(100.0, calc.storedKwh, 1e-9)

        vm.onSocChange("150")
        advanceUntilIdle()
        assertNull(vm.calculation.value)
        assertNotNull(vm.errors.value[CalcField.SOC])
    }

    @Test
    fun simulator_respectsEfficiencyAndLimits() {
        // 100 кВт × 1 год × 0,95 = 95 кВт·год = 19 % від 500 кВт·год
        assertEquals(69.0, BatterySimulator.step(50.0, BatteryMode.CHARGING, 100.0, 1.0), 1e-9)
        assertEquals(100.0, BatterySimulator.step(99.0, BatteryMode.CHARGING, 250.0, 1.0), 1e-9)
        assertEquals(0.0, BatterySimulator.step(1.0, BatteryMode.DISCHARGING, 250.0, 1.0), 1e-9)
        assertEquals(50.0, BatterySimulator.step(50.0, BatteryMode.IDLE, 250.0, 1.0), 1e-9)
    }

    @Test
    fun analyzer_detectsOverheatingAndTrend() {
        val history = listOf(reading(50.0, 30.0), reading(55.0, 41.0))
        val insights = BatteryAnalyzer.analyze(history)
        assertEquals(HealthStatus.CRITICAL, insights.status)
        assertEquals(SocTrend.RISING, insights.trend)
        assertEquals(41.0, insights.peakTemperatureC, 1e-9)
    }

    @Test
    fun viewModel_publishesSuccessThenError() = runTest(dispatcher) {
        val source = FakeSource()
        val vm = TelemetryViewModel(source)
        assertEquals(TelemetryUiState.Loading, vm.uiState.value)
        advanceUntilIdle()
        val success = vm.uiState.value as TelemetryUiState.Success
        assertEquals(1, success.history.size)

        source.fail = true
        vm.refresh()
        advanceUntilIdle()
        val error = vm.uiState.value as TelemetryUiState.Error
        assertEquals("offline", error.message)
        // Попередні дані зберігаються, щоб UI міг їх показати разом з помилкою
        assertEquals(success.latest, error.previous?.latest)
    }

    private class FakeSource : TelemetrySource {
        var fail = false
        override suspend fun fetch(): TelemetryReading {
            if (fail) throw IOException("offline")
            return reading(60.0, 25.0)
        }
    }

    private companion object {
        fun reading(soc: Double, temperature: Double) = TelemetryReading(0L, soc, 50.0, temperature, 760.0)
    }
}
