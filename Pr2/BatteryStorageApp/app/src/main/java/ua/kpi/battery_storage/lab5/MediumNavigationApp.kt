package ua.kpi.battery_storage.lab5

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ua.kpi.battery_storage.model.BatteryStatus
import ua.kpi.battery_storage.ui.components.InfoRow
import ua.kpi.battery_storage.ui.components.formatOneDecimal
import ua.kpi.battery_storage.ui.components.formatPower

private const val DEFAULT_CHARGE_LIMIT = 90

/**
 * Середній рівень ПЗ 5: три екрани (стан, оператор, налаштування з формою).
 * Дані передаються вперед через аргументи маршруту, а назад — через SavedStateHandle,
 * тож після повернення головне меню одразу показує нову межу заряду.
 */
@Composable
fun MediumNavigationApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Medium.Menu.route) {
        composable(Screen.Medium.Menu.route) { entry ->
            // Результат з екрана налаштувань читається як StateFlow — UI оновлюється автоматично
            val chargeLimit by entry.savedStateHandle
                .getStateFlow(Screen.RESULT_CHARGE_LIMIT, DEFAULT_CHARGE_LIMIT)
                .collectAsState()

            MediumMainMenu(
                chargeLimit = chargeLimit,
                onStatusClick = { soc, power ->
                    navController.navigate(Screen.Medium.Status.createRoute(soc, power, chargeLimit))
                },
                onOperatorClick = { navController.navigate(Screen.Medium.Operator.route) },
                onSettingsClick = { navController.navigate(Screen.Medium.Settings.createRoute(chargeLimit)) },
            )
        }

        composable(
            route = Screen.Medium.Status.route,
            arguments = listOf(
                navArgument(Screen.ARG_SOC) { type = NavType.IntType },
                navArgument(Screen.ARG_POWER) { type = NavType.IntType },
                navArgument(Screen.ARG_LIMIT) { type = NavType.IntType },
            ),
        ) { entry ->
            val args = requireNotNull(entry.arguments)
            MediumStatusScreen(
                status = BatteryStatus(
                    stateOfCharge = args.getInt(Screen.ARG_SOC),
                    powerKw = args.getInt(Screen.ARG_POWER),
                    temperatureC = 27.0,
                ),
                chargeLimit = args.getInt(Screen.ARG_LIMIT),
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Medium.Operator.route) {
            MediumOperatorScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Medium.Settings.route,
            arguments = listOf(navArgument(Screen.ARG_LIMIT) { type = NavType.IntType }),
        ) { entry ->
            MediumSettingsScreen(
                initialLimit = entry.arguments?.getInt(Screen.ARG_LIMIT) ?: DEFAULT_CHARGE_LIMIT,
                onSave = { newLimit ->
                    // Передаємо результат попередньому екрану й повертаємося назад
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(Screen.RESULT_CHARGE_LIMIT, newLimit)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

/** Головне меню: картка поточних показників (Row + Column) і три кнопки переходу. */
@Composable
private fun MediumMainMenu(
    chargeLimit: Int,
    onStatusClick: (soc: Int, powerKw: Int) -> Unit,
    onOperatorClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    // Показники симулюються в меню і передаються на екран стану як аргументи
    var soc by rememberSaveable { mutableIntStateOf(BatteryStatus.Initial.stateOfCharge) }
    var power by rememberSaveable { mutableIntStateOf(120) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "🔋 Battery Storage",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Рівень заряду", style = MaterialTheme.typography.labelMedium)
                    Text("$soc %", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Межа заряду", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "$chargeLimit %",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                val next = BatteryStatus.random()
                soc = next.stateOfCharge
                power = next.powerKw
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Оновити телеметрію") }

        Button(onClick = { onStatusClick(soc, power) }, modifier = Modifier.fillMaxWidth()) {
            Text("Стан батареї")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onOperatorClick, modifier = Modifier.weight(1f)) { Text("Оператор") }
            Button(onClick = onSettingsClick, modifier = Modifier.weight(1f)) { Text("Налаштування") }
        }
    }
}

/** Екран стану: відображає значення, отримані через аргументи NavController. */
@Composable
private fun MediumStatusScreen(status: BatteryStatus, chargeLimit: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Стан батареї", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Отримано через аргументи маршруту",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("Рівень заряду", "${status.stateOfCharge} %")
                InfoRow("Потужність", formatPower(status.powerKw))
                InfoRow("Режим", status.mode.title)
                InfoRow(
                    "Накопичено",
                    "${status.storedEnergyKwh.formatOneDecimal()} / ${BatteryStatus.CAPACITY_KWH} кВт·год",
                )
                InfoRow("Межа заряду", "$chargeLimit %")
            }
        }
        if (status.stateOfCharge > chargeLimit) {
            Text(
                "Рівень заряду перевищує встановлену межу — заряджання слід зупинити.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

/** Екран оператора: дані згруповано рядками Row усередині Column. */
@Composable
private fun MediumOperatorScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Оператор системи", style = MaterialTheme.typography.headlineSmall)
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("ПІБ", "Лях В. О.")
                InfoRow("Посада", "Інженер BESS")
                InfoRow("Зміна", "08:00 – 20:00")
                InfoRow("Об'єкт", "Накопичувач 500 кВт·год")
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

/**
 * Екран налаштувань із формою: поле для межі заряду з перевіркою діапазону 50–100 %.
 * Збережене значення повертається в головне меню.
 */
@Composable
private fun MediumSettingsScreen(initialLimit: Int, onSave: (Int) -> Unit, onCancel: () -> Unit) {
    var input by rememberSaveable { mutableStateOf(initialLimit.toString()) }
    val value = input.toIntOrNull()
    val error = when {
        input.isBlank() -> "Введіть значення"
        value == null -> "Потрібне ціле число"
        value !in 50..100 -> "Допустимий діапазон: 50–100 %"
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Налаштування заряду", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter(Char::isDigit).take(3) },
            label = { Text("Межа заряду, %") },
            isError = error != null,
            supportingText = { Text(error ?: "Заряджання зупиняється при досягненні цього рівня") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Скасувати") }
            Button(
                onClick = { value?.let(onSave) },
                enabled = error == null,
                modifier = Modifier.weight(1f),
            ) { Text("Зберегти") }
        }
    }
}
