package ua.kpi.battery_storage.lab8

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.battery_storage.lab8.advanced.data.RemoteEnergySource
import ua.kpi.battery_storage.lab8.advanced.data.StorageUnitEntity
import ua.kpi.battery_storage.lab8.advanced.data.UnitSummary
import ua.kpi.battery_storage.lab8.advanced.repository.FleetAnalytics
import ua.kpi.battery_storage.lab8.home.MeasurementField
import ua.kpi.battery_storage.lab8.home.MeasurementValidator
import kotlin.random.Random

class Lab8LogicTest {

    @Test
    fun measurementValidator_checksRanges() {
        assertTrue(MeasurementValidator.validate("50", "-120", "25,5").isEmpty())
        val errors = MeasurementValidator.validate("101", "300", "")
        assertEquals(setOf(MeasurementField.SOC, MeasurementField.POWER, MeasurementField.TEMPERATURE), errors.keys)
    }

    @Test
    fun unitSummary_computedFields() {
        val s = summary("A", capacity = 500.0, latestSoc = 40.0, charged = 400.0, discharged = 360.0, maxTemp = 30.0)
        assertEquals(200.0, s.storedKwh, 1e-9)
        assertEquals(0.9, s.roundTripEfficiency!!, 1e-9)
        assertEquals(0.72, s.equivalentCycles, 1e-9)
        assertNull(summary("B", 100.0, null, 0.0, 0.0, 20.0).roundTripEfficiency)
    }

    @Test
    fun fleetAnalytics_weightsSocByCapacity() {
        val stats = FleetAnalytics.aggregate(
            listOf(
                summary("A", 500.0, 80.0, 100.0, 50.0, 31.0),
                summary("B", 1000.0, 20.0, 100.0, 50.0, 36.0),
            ),
        )
        // (400 + 200) / 1500 = 40 %
        assertEquals(40.0, stats.fleetSoc, 1e-9)
        assertEquals("B", stats.hottestUnit?.unitName)
        assertEquals(0.5, stats.roundTripEfficiency!!, 1e-9)
    }

    @Test
    fun generatedSeries_staysWithinPhysicalLimits() {
        val unit = StorageUnitEntity(id = 1, siteId = 1, name = "U", capacityKwh = 100.0, maxPowerKw = 100.0)
        val series = RemoteEnergySource(Random(3)).generateSeries(unit, from = 0L, points = 200, startSoc = 50.0)
        assertTrue(series.all { it.stateOfCharge in 0.0..100.0 })
        assertTrue(series.zipWithNext().all { (a, b) -> b.recordedAt - a.recordedAt == 15 * 60_000L })
    }

    private fun summary(name: String, capacity: Double, latestSoc: Double?, charged: Double, discharged: Double, maxTemp: Double) =
        UnitSummary(1, name, "Site", capacity, capacity / 2, 10, latestSoc, latestSoc ?: 0.0, maxTemp, charged, discharged)
}
