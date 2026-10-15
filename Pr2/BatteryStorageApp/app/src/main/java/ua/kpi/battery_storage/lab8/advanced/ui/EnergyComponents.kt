package ua.kpi.battery_storage.lab8.advanced.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.kpi.battery_storage.lab8.advanced.data.RecordWithUnit
import ua.kpi.battery_storage.lab8.advanced.data.UnitSummary
import ua.kpi.battery_storage.lab8.advanced.repository.FleetStatistics
import ua.kpi.battery_storage.lab8.formatDateTime
import ua.kpi.battery_storage.ui.components.InfoRow
import ua.kpi.battery_storage.ui.components.SectionCard
import ua.kpi.battery_storage.ui.components.chargeColor
import ua.kpi.battery_storage.ui.components.formatOneDecimal
import ua.kpi.battery_storage.ui.components.formatPower
import kotlin.math.roundToInt

/**
 * Універсальний ряд чипів з одиничним вибором; використовується для майданчиків,
 * режимів і сортування. Ряд прокручується горизонтально на вузьких екранах.
 */
@Composable
fun <T> ChipRow(
    label: String,
    options: List<T>,
    selected: T,
    title: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(title(option)) })
            }
        }
    }
}

/** Зведена статистика парку (результат бізнес-логіки FleetAnalytics). */
@Composable
fun FleetStatsCard(stats: FleetStatistics) {
    SectionCard(title = "Парк накопичувачів · ${stats.unitCount} уст.") {
        val soc = stats.fleetSoc.roundToInt()
        LinearProgressIndicator(progress = { soc / 100f }, color = chargeColor(soc), modifier = Modifier.fillMaxWidth())
        InfoRow("Зважений SoC", "${stats.fleetSoc.formatOneDecimal()} %")
        InfoRow("Накопичено", "${stats.totalStoredKwh.formatOneDecimal()} / ${stats.totalCapacityKwh.roundToInt()} кВт·год")
        InfoRow("Прийнято / віддано", "${stats.chargedKwh.roundToInt()} / ${stats.dischargedKwh.roundToInt()} кВт·год")
        InfoRow("ККД циклу", stats.roundTripEfficiency?.let { "${(it * 100).formatOneDecimal()} %" } ?: "—")
        stats.hottestUnit?.let {
            InfoRow("Найгарячіша", "${it.unitName}: ${it.maxTemperature.formatOneDecimal()} °C")
        }
    }
}

/** Картка установки з обчислюваними полями; виділена рамкою, якщо за нею фільтруються записи. */
@Composable
fun UnitSummaryCard(summary: UnitSummary, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val soc = summary.latestSoc?.roundToInt() ?: 0
    OutlinedCard(
        onClick = onClick,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.width(200.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(summary.unitName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${summary.siteName} · ${summary.capacityKwh.roundToInt()} кВт·год", style = MaterialTheme.typography.bodySmall)
            Text("SoC $soc %", color = chargeColor(soc), fontWeight = FontWeight.SemiBold)
            Text("Середній ${summary.avgSoc.formatOneDecimal()} % · макс ${summary.maxTemperature.formatOneDecimal()} °C", style = MaterialTheme.typography.bodySmall)
            Text(
                "Цикли ${summary.equivalentCycles.formatOneDecimal()} · записів ${summary.recordCount}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Рядок списку записів телеметрії. */
@Composable
fun RecordRow(item: RecordWithUnit) {
    val r = item.record
    val soc = r.stateOfCharge.roundToInt()
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.unitName, style = MaterialTheme.typography.labelLarge)
                Text(formatDateTime(r.recordedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${r.stateOfCharge.formatOneDecimal()} %", color = chargeColor(soc), fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
            Text(formatPower(r.powerKw), modifier = Modifier.width(76.dp), style = MaterialTheme.typography.bodySmall)
            Text("${r.temperatureC.formatOneDecimal()} °C", style = MaterialTheme.typography.bodySmall)
        }
    }
}
