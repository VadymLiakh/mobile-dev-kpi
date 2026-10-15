package ua.kpi.battery_storage.lab7.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.battery_storage.model.BatteryMode
import ua.kpi.battery_storage.ui.components.BatteryIndicator
import ua.kpi.battery_storage.ui.components.InfoRow
import ua.kpi.battery_storage.ui.components.SectionCard
import ua.kpi.battery_storage.ui.components.chargeColor
import ua.kpi.battery_storage.ui.components.formatOneDecimal
import kotlin.math.roundToInt

/**
 * VIEW домашнього завдання ПЗ 7: моніторинг заряджання / розряджання накопичувача.
 * Екран читає uiState з ViewModel і викликає її методи; власного стану не має.
 */
@Composable
fun HomeMvvmApp(viewModel: BatteryMonitorViewModel = viewModel()) {
    val state = viewModel.uiState
    val animatedSoc by animateFloatAsState(state.stateOfCharge.toFloat(), label = "soc")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BatteryIndicator(
            fraction = animatedSoc / 100f,
            fillColor = chargeColor(animatedSoc.roundToInt()),
            outlineColor = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "${state.stateOfCharge.formatOneDecimal()} %",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )

        SectionCard(title = "Параметри") {
            InfoRow("Накопичено", "${state.storedKwh.formatOneDecimal()} кВт·год")
            InfoRow("Режим", state.mode.title)
            InfoRow("Модельний час", "${state.simulatedMinutes / 60} год ${state.simulatedMinutes % 60} хв")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BatteryMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { viewModel.selectMode(mode) },
                    label = { Text(mode.title) },
                )
            }
        }

        Text("Потужність: ${state.powerKw.roundToInt()} кВт", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = state.powerKw,
            onValueChange = viewModel::setPower,
            valueRange = 10f..250f,
            enabled = state.mode != BatteryMode.IDLE,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = viewModel::toggleSimulation, modifier = Modifier.weight(1f)) {
                Text(if (state.running) "Зупинити" else "Запустити")
            }
            OutlinedButton(onClick = viewModel::reset, modifier = Modifier.weight(1f)) { Text("Скинути") }
        }

        SectionCard(title = "Журнал подій") {
            if (state.log.isEmpty()) Text("Подій ще немає", style = MaterialTheme.typography.bodySmall)
            state.log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
