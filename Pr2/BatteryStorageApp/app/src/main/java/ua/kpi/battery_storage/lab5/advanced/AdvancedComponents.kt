package ua.kpi.battery_storage.lab5.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.kpi.battery_storage.ui.components.chargeColor
import ua.kpi.battery_storage.ui.components.formatOneDecimal

/**
 * Повторно використовувана кнопка головного меню: іконка, назва та короткий опис.
 * Уся картка є клікабельною, що зручно на сенсорних екранах.
 */
@Composable
fun MenuButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Зведена картка стану: користувач, рівень заряду системи та збережений діапазон.
 * Демонструє інтеграцію UI з бізнес-логікою BatteryControlState.
 */
@Composable
fun SystemSummaryCard(state: BatteryControlState, modifier: Modifier = Modifier) {
    val soc = state.averageSoc
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${state.profile.name} · ${state.profile.role.title}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (state.profile.onDuty) "На зміні" else "Не на зміні",
                style = MaterialTheme.typography.labelMedium,
                color = if (state.profile.onDuty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Заряд системи: $soc %",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { soc / 100f },
                color = chargeColor(soc),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Робочий діапазон: ${state.settings.minSoc}–${state.settings.maxSoc} %  ·  " +
                    "${state.storedEnergyKwh.formatOneDecimal()} кВт·год",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Список попереджень; критичні виділено кольором помилки. */
@Composable
fun AlertList(alerts: List<BatteryAlert>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (alerts.isEmpty()) {
            Text("Попереджень немає", color = MaterialTheme.colorScheme.primary)
        }
        alerts.forEach { alert ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (alert.critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = alert.text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/** Картка модуля для сітки на панелі стану; натискання відкриває деталі модуля. */
@Composable
fun ModuleCard(module: BatteryModuleState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(module.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${module.soc} %",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = chargeColor(module.soc),
            )
            LinearProgressIndicator(
                progress = { module.soc / 100f },
                color = chargeColor(module.soc),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${module.temperatureC.formatOneDecimal()} °C",
                style = MaterialTheme.typography.bodySmall,
                color = if (module.temperatureC > BatteryModuleState.MAX_SAFE_TEMPERATURE) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
