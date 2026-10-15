package ua.kpi.battery_storage.lab8.home

/** Поля форми журналу вимірювань. */
enum class MeasurementField { SOC, POWER, TEMPERATURE }

/** Базова валідація введених значень перед збереженням у Room. */
object MeasurementValidator {
    const val MAX_POWER_KW = 250

    fun validate(soc: String, power: String, temperature: String): Map<MeasurementField, String> = buildMap {
        val s = soc.toIntOrNull()
        if (s == null || s !in 0..100) put(MeasurementField.SOC, "Ціле 0–100")
        val p = power.toIntOrNull()
        if (p == null || p !in -MAX_POWER_KW..MAX_POWER_KW) put(MeasurementField.POWER, "Ціле ±$MAX_POWER_KW")
        val t = temperature.replace(',', '.').toDoubleOrNull()
        if (t == null || t !in -20.0..60.0) put(MeasurementField.TEMPERATURE, "−20…60 °C")
    }
}
