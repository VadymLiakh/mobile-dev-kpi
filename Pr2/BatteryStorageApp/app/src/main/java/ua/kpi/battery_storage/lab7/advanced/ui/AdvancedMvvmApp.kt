package ua.kpi.battery_storage.lab7.advanced.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.battery_storage.lab7.advanced.domain.BatteryAnalyzer
import ua.kpi.battery_storage.lab7.advanced.viewmodel.TelemetryUiState
import ua.kpi.battery_storage.lab7.advanced.viewmodel.TelemetryViewModel
import ua.kpi.battery_storage.ui.components.BatteryIndicator
import ua.kpi.battery_storage.ui.components.chargeColor
import ua.kpi.battery_storage.ui.components.formatOneDecimal
import ua.kpi.battery_storage.ui.components.formatPower
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Просунутий рівень ПЗ 7. View підписується на StateFlow через collectAsStateWithLifecycle
 * (збирання зупиняється, коли застосунок у фоні) і передає дії користувача у ViewModel.
 */
@Composable
fun AdvancedMvvmApp(viewModel: TelemetryViewModel = viewModel(factory = TelemetryViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val autoRefresh by viewModel.autoRefresh.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ControlBar(
            autoRefresh = autoRefresh,
            refreshing = uiState is TelemetryUiState.Loading ||
                (uiState as? TelemetryUiState.Success)?.refreshing == true,
            onRefresh = viewModel::refresh,
            onToggleAuto = viewModel::toggleAutoRefresh,
        )
        // Кожному стану ViewModel відповідає свій UI
        when (val state = uiState) {
            TelemetryUiState.Loading -> LoadingState()
            is TelemetryUiState.Success -> TelemetryContent(state)
            is TelemetryUiState.Error -> Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ErrorState(state.message, onRetry = viewModel::refresh)
                // Якщо раніше були дані — показуємо їх під повідомленням про помилку
                state.previous?.let { TelemetryContent(it) }
            }
        }
    }
}

/** Панель керування: ручне оновлення та перемикач автооновлення. */
@Composable
private fun ControlBar(autoRefresh: Boolean, refreshing: Boolean, onRefresh: () -> Unit, onToggleAuto: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onRefresh, enabled = !refreshing) { Text("Оновити") }
            Text("Автооновлення", modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp), textAlign = TextAlign.End)
            Switch(checked = autoRefresh, onCheckedChange = { onToggleAuto() }, modifier = Modifier.padding(start = 8.dp))
        }
        // Тонка смуга показує, що йде фоновий запит, не перекриваючи дані
        if (refreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Адаптивний вміст: на вузькому екрані — одна колонка,
 * на широкому — індикатор і показники ліворуч, графік і попередження праворуч.
 */
@Composable
private fun TelemetryContent(state: TelemetryUiState.Success) {
    val reading = state.latest
    val socInt = reading.stateOfCharge.roundToInt()

    val overview: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            BatteryIndicator(
                fraction = (reading.stateOfCharge / 100).toFloat(),
                fillColor = chargeColor(socInt),
                outlineColor = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "${reading.stateOfCharge.formatOneDecimal()} % · ${state.insights.trend.title}",
                style = MaterialTheme.typography.titleLarge,
            )
            Text("Оновлено о ${TimeFormat.format(Date(reading.timestampMillis))}", style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Потужність", formatPower(reading.powerKw), Modifier.weight(1f))
            MetricTile(
                "Температура",
                "${reading.temperatureC.formatOneDecimal()} °C",
                Modifier.weight(1f),
                caption = "пік ${state.insights.peakTemperatureC.formatOneDecimal()} °C",
                valueColor = if (reading.temperatureC >= BatteryAnalyzer.TEMP_WARNING) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Напруга DC", "${reading.voltageV.roundToInt()} В", Modifier.weight(1f))
            MetricTile("Середній SoC", "${state.insights.averageSoc.formatOneDecimal()} %", Modifier.weight(1f))
        }
    }
    val analysis: @Composable () -> Unit = {
        StatusBanner(state.insights)
        SocHistoryChart(state.history.map { it.stateOfCharge })
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 600.dp) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { overview() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { analysis() }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                overview()
                analysis()
            }
        }
    }
}

private val TimeFormat = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("uk-UA"))
