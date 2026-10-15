package ua.kpi.battery_storage.lab5.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.kpi.battery_storage.ui.components.BatteryIndicator
import ua.kpi.battery_storage.ui.components.InfoRow
import ua.kpi.battery_storage.ui.components.ScreenTitle
import ua.kpi.battery_storage.ui.components.SectionCard
import ua.kpi.battery_storage.ui.components.chargeColor
import ua.kpi.battery_storage.ui.components.formatOneDecimal
import ua.kpi.battery_storage.ui.components.formatPower
import kotlin.math.roundToInt

/** Ширина, починаючи з якої застосовується двоколонковий (планшетний) макет. */
private val WideLayoutBreakpoint = 600.dp

/**
 * Головне меню просунутого рівня. Адаптивний макет: на вузькому екрані зведення
 * та кнопки йдуть одне під одним, на широкому — у дві колонки.
 */
@Composable
fun AdvancedMainMenuScreen(
    state: BatteryControlState,
    onDashboard: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideLayoutBreakpoint

        // Кнопки меню описано один раз і повторно використано в обох макетах
        val menu: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MenuButton("Стан системи", "Телеметрія та модулі", Icons.Filled.Info, onDashboard)
                MenuButton("Налаштування заряду", "Робочий діапазон SoC", Icons.Filled.Settings, onSettings)
                MenuButton("Профіль оператора", "Роль і статус зміни", Icons.Filled.Person, onProfile)
            }
        }
        val summary: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemSummaryCard(state)
                SectionCard(title = "Попередження") { AlertList(state.alerts) }
            }
        }

        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                summary(Modifier.weight(1f).verticalScroll(rememberScrollState()))
                menu(Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                summary(Modifier)
                menu(Modifier)
            }
        }
    }
}

/**
 * Панель стану: індикатор батареї, сумарні показники та сітка модулів.
 * LazyVerticalGrid з GridCells.Adaptive сам підбирає кількість колонок під ширину екрана.
 */
@Composable
fun DashboardScreen(state: BatteryControlState, onModuleClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BatteryIndicator(
                    fraction = state.averageSoc / 100f,
                    fillColor = chargeColor(state.averageSoc),
                    outlineColor = MaterialTheme.colorScheme.onBackground,
                )
                SectionCard(title = "Система", modifier = Modifier.padding(top = 12.dp)) {
                    InfoRow("Рівень заряду", "${state.averageSoc} %")
                    InfoRow("Потужність", formatPower(state.totalPowerKw))
                    InfoRow("Накопичено", "${state.storedEnergyKwh.formatOneDecimal()} кВт·год")
                }
                OutlinedButton(
                    onClick = state::refreshTelemetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) { Text("Оновити телеметрію") }
            }
        }
        items(state.modules, key = { it.id }) { module ->
            ModuleCard(module = module, onClick = { onModuleClick(module.id) })
        }
    }
}

/** Деталі модуля; ідентифікатор модуля приходить як аргумент маршруту. */
@Composable
fun ModuleDetailsScreen(state: BatteryControlState, moduleId: Int) {
    val module = state.module(moduleId)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (module == null) {
            Text("Модуль #$moduleId не знайдено", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        ScreenTitle(module.name, subtitle = "Ємність ${BatteryModuleState.MODULE_CAPACITY_KWH} кВт·год")
        SectionCard(title = "Телеметрія") {
            InfoRow("Рівень заряду", "${module.soc} %", valueColor = chargeColor(module.soc))
            InfoRow("Накопичено", "${module.storedEnergyKwh.formatOneDecimal()} кВт·год")
            InfoRow("Потужність", formatPower(module.powerKw))
            InfoRow("Температура", "${module.temperatureC.formatOneDecimal()} °C")
        }
        val withinRange = module.soc in state.settings.minSoc..state.settings.maxSoc
        Text(
            text = if (withinRange) "Модуль працює в робочому діапазоні" else "Модуль поза робочим діапазоном",
            color = if (withinRange) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

/**
 * Налаштування робочого діапазону заряду. Перевірку прав і значень виконує
 * BatteryControlState.applySettings, екран лише показує результат.
 */
@Composable
fun ChargeSettingsScreen(state: BatteryControlState, onSaved: () -> Unit) {
    var range by remember {
        mutableStateOf(state.settings.minSoc.toFloat()..state.settings.maxSoc.toFloat())
    }
    var error by remember { mutableStateOf<String?>(null) }
    val minSoc = range.start.roundToInt()
    val maxSoc = range.endInclusive.roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Робочий діапазон", subtitle = "Мінімальний і максимальний рівень заряду")
        Text("$minSoc % – $maxSoc %", style = MaterialTheme.typography.headlineSmall)
        RangeSlider(
            value = range,
            onValueChange = { range = it; error = null },
            valueRange = 0f..100f,
            steps = 19,
            enabled = state.canEditSettings,
        )
        if (!state.canEditSettings) {
            Text(
                "Редагування недоступне: змініть роль або статус зміни у профілі.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                error = state.applySettings(minSoc, maxSoc)
                if (error == null) onSaved()
            },
            enabled = state.canEditSettings,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Зберегти") }
    }
}

/** Профіль оператора: ім'я, роль і статус зміни впливають на доступ до налаштувань. */
@Composable
fun OperatorProfileScreen(state: BatteryControlState) {
    val profile = state.profile
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Профіль", subtitle = "Зміни застосовуються одразу")
        OutlinedTextField(
            value = profile.name,
            onValueChange = { state.updateProfile(profile.copy(name = it)) },
            label = { Text("Ім'я оператора") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Роль", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperatorRole.entries.forEach { role ->
                FilterChip(
                    selected = profile.role == role,
                    onClick = { state.updateProfile(profile.copy(role = role)) },
                    label = { Text(role.title) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("На зміні", modifier = Modifier.weight(1f))
            Switch(
                checked = profile.onDuty,
                onCheckedChange = { state.updateProfile(profile.copy(onDuty = it)) },
            )
        }
        InfoRow(
            "Доступ до налаштувань",
            if (state.canEditSettings) "дозволено" else "заборонено",
            valueColor = if (state.canEditSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
