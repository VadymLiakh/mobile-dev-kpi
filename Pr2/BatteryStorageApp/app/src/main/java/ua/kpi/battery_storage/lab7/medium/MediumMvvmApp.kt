package ua.kpi.battery_storage.lab7.medium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ua.kpi.battery_storage.model.BatteryMode
import ua.kpi.battery_storage.ui.components.InfoRow
import ua.kpi.battery_storage.ui.components.SectionCard
import ua.kpi.battery_storage.ui.components.chargeColor
import ua.kpi.battery_storage.ui.components.formatDecimals
import ua.kpi.battery_storage.ui.components.formatOneDecimal
import ua.kpi.battery_storage.ui.components.formatPower
import kotlin.math.roundToInt

/**
 * Середній рівень ПЗ 7: калькулятор накопичувача. Стан повністю живе у ViewModel,
 * а два екрани (параметри і прогноз) лише спостерігають за StateFlow.
 */
@Composable
fun MediumMvvmApp(viewModel: MediumBatteryViewModel = viewModel()) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "medium_mvvm_calc") {
        composable("medium_mvvm_calc") {
            CalculatorScreen(viewModel, onForecast = { navController.navigate("medium_mvvm_forecast") })
        }
        composable("medium_mvvm_forecast") {
            ForecastScreen(viewModel, onBack = { navController.popBackStack() })
        }
    }
}

/** Екран параметрів: компонент введення та компонент результатів. */
@Composable
private fun CalculatorScreen(viewModel: MediumBatteryViewModel, onForecast: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InputPanel(viewModel)
        ResultPanel(viewModel)
        Button(onClick = onForecast, modifier = Modifier.fillMaxWidth()) { Text("Погодинний прогноз") }
    }
}

/** Компонент введення: значення та помилки беруться з різних StateFlow однієї ViewModel. */
@Composable
private fun InputPanel(viewModel: MediumBatteryViewModel) {
    val capacity by viewModel.capacity.collectAsStateWithLifecycle()
    val soc by viewModel.soc.collectAsStateWithLifecycle()
    val power by viewModel.power.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()

    SectionCard(title = "Параметри накопичувача") {
        NumberField(capacity, viewModel::onCapacityChange, "Ємність, кВт·год", errors[CalcField.CAPACITY])
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(soc, viewModel::onSocChange, "Заряд, %", errors[CalcField.SOC], Modifier.weight(1f))
            NumberField(power, viewModel::onPowerChange, "Потужність, кВт", errors[CalcField.POWER], Modifier.weight(1f))
        }
        OutlinedButton(onClick = viewModel::reversePower, modifier = Modifier.fillMaxWidth()) {
            Text("Змінити напрям: заряд ↔ розряд")
        }
    }
}

/** Компонент результатів: похідні величини з calculation, оновлюються при кожному введенні. */
@Composable
private fun ResultPanel(viewModel: MediumBatteryViewModel) {
    val calc by viewModel.calculation.collectAsStateWithLifecycle()

    SectionCard(title = "Розрахунок") {
        val c = calc
        if (c == null) {
            Text("Виправте помилки у параметрах", color = MaterialTheme.colorScheme.error)
            return@SectionCard
        }
        val socInt = c.stateOfCharge.roundToInt()
        LinearProgressIndicator(
            progress = { (c.stateOfCharge / 100).toFloat() },
            color = chargeColor(socInt),
            modifier = Modifier.fillMaxWidth(),
        )
        InfoRow("Режим", c.mode.title)
        InfoRow("Накопичено", "${c.storedKwh.formatOneDecimal()} кВт·год")
        InfoRow("Вільна ємність", "${c.freeKwh.formatOneDecimal()} кВт·год")
        InfoRow("C-rate", "${c.cRate.formatDecimals(2)}C")
        InfoRow(
            when (c.mode) {
                BatteryMode.CHARGING -> "До повного заряду"
                else -> "До повного розряду"
            },
            c.hoursToLimit?.let { formatHours(it) } ?: "—",
        )
        c.warnings.forEach { Text("⚠ $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

/** Другий екран: прогноз рівня заряду, обчислений у ViewModel з розрахунку. */
@Composable
private fun ForecastScreen(viewModel: MediumBatteryViewModel, onBack: () -> Unit) {
    val projection by viewModel.projection.collectAsStateWithLifecycle()
    val calc by viewModel.calculation.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Прогноз заряду", style = MaterialTheme.typography.headlineSmall)
        Text(
            calc?.let { "Потужність ${formatPower(it.powerKw)}, режим: ${it.mode.title.lowercase()}" } ?: "Немає даних",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (projection.size <= 1) {
            Text("Батарея в режимі очікування або вже на межі — заряд не змінюється.")
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(projection) { (hour, soc) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("+$hour год", modifier = Modifier.padding(end = 12.dp), style = MaterialTheme.typography.labelLarge)
                    LinearProgressIndicator(
                        progress = { (soc / 100).toFloat() },
                        color = chargeColor(soc.roundToInt()),
                        modifier = Modifier.weight(1f),
                    )
                    Text("${soc.formatOneDecimal()} %", modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
}

/** 2,75 год → «2 год 45 хв». */
private fun formatHours(hours: Double): String {
    val totalMinutes = (hours * 60).roundToInt()
    return "${totalMinutes / 60} год ${totalMinutes % 60} хв"
}
