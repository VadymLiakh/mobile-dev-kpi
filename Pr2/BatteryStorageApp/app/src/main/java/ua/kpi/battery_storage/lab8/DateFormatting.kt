package ua.kpi.battery_storage.lab8

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DateTimeFormat = SimpleDateFormat("dd.MM HH:mm:ss", Locale.forLanguageTag("uk-UA"))

/** Час запису у форматі «15.10 21:30:05». */
fun formatDateTime(millis: Long): String = DateTimeFormat.format(Date(millis))
