package ua.kpi.battery_storage.composables

import java.util.Locale
import kotlin.math.abs

private val UkrainianLocale: Locale = Locale.forLanguageTag("uk-UA")

/** Число з одним знаком після коми в українському форматі: 12,5. */
fun Double.formatOneDecimal(): String = String.format(UkrainianLocale, "%.1f", this)

/** Потужність зі знаком напрямку: «+120 кВт» (заряджання), «−80 кВт» (розряджання). */
fun formatPower(powerKw: Number): String {
    val value = powerKw.toDouble()
    val sign = when {
        value > 0.5 -> "+"
        value < -0.5 -> "−"
        else -> ""
    }
    return "$sign${abs(value).toInt()} кВт"
}
