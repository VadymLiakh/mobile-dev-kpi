package ua.kpi.battery_storage.lab7.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ua.kpi.battery_storage.ui.components.BatteryIndicator
import ua.kpi.battery_storage.ui.components.chargeColor

/**
 * ViewModel базового рівня ПЗ 7: зберігає два значення — назву системи та рівень заряду.
 * Дані доступні UI лише для читання (LiveData), змінюються через методи ViewModel.
 * ViewModel переживає поворот екрана, тому введені значення не втрачаються.
 */
class BasicBatteryViewModel : ViewModel() {
    private val _systemName = MutableLiveData("BESS-500")
    val systemName: LiveData<String> = _systemName

    private val _stateOfCharge = MutableLiveData(50)
    val stateOfCharge: LiveData<Int> = _stateOfCharge

    fun rename(name: String) {
        if (name.isNotBlank()) _systemName.value = name.trim()
    }

    /** Встановлює рівень заряду, обмежуючи його діапазоном 0–100 %. */
    fun setStateOfCharge(value: Int) {
        _stateOfCharge.value = value.coerceIn(0, 100)
    }

    fun changeStateOfCharge(delta: Int) = setStateOfCharge((_stateOfCharge.value ?: 0) + delta)
}

/**
 * Базовий рівень ПЗ 7. Одна ViewModel спільна для двох екранів:
 * на першому користувач вводить дані, на другому бачить їх у вигляді індикатора.
 */
@Composable
fun BasicMvvmApp(viewModel: BasicBatteryViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "basic_mvvm_input") {
        composable("basic_mvvm_input") {
            InputScreen(viewModel, onShowIndicator = { navController.navigate("basic_mvvm_indicator") })
        }
        composable("basic_mvvm_indicator") {
            IndicatorScreen(viewModel, onBack = { navController.popBackStack() })
        }
    }
}

/** Екран введення: TextField для назви та рівня заряду, поточні значення з LiveData. */
@Composable
private fun InputScreen(viewModel: BasicBatteryViewModel, onShowIndicator: () -> Unit) {
    // observeAsState перетворює LiveData на Compose State: UI оновлюється при кожній зміні
    val name by viewModel.systemName.observeAsState("")
    val soc by viewModel.stateOfCharge.observeAsState(0)

    var nameInput by rememberSaveable { mutableStateOf("") }
    var socInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Дані у ViewModel", style = MaterialTheme.typography.titleMedium)
        Text(name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("Рівень заряду: $soc %", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Нова назва системи") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = socInput,
            onValueChange = { socInput = it.filter(Char::isDigit).take(3) },
            label = { Text("Рівень заряду, %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                viewModel.rename(nameInput)
                socInput.toIntOrNull()?.let(viewModel::setStateOfCharge)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Зберегти у ViewModel") }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.changeStateOfCharge(-10) }, modifier = Modifier.weight(1f)) {
                Text("−10 %")
            }
            OutlinedButton(onClick = { viewModel.changeStateOfCharge(10) }, modifier = Modifier.weight(1f)) {
                Text("+10 %")
            }
        }
        Button(onClick = onShowIndicator, modifier = Modifier.fillMaxWidth()) { Text("Показати індикатор") }
    }
}

/** Другий екран: ті самі дані з тієї ж ViewModel у графічному вигляді. */
@Composable
private fun IndicatorScreen(viewModel: BasicBatteryViewModel, onBack: () -> Unit) {
    val name by viewModel.systemName.observeAsState("")
    val soc by viewModel.stateOfCharge.observeAsState(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(name, style = MaterialTheme.typography.headlineSmall)
        BatteryIndicator(
            fraction = soc / 100f,
            fillColor = chargeColor(soc),
            outlineColor = MaterialTheme.colorScheme.onBackground,
        )
        Text("$soc %", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.changeStateOfCharge(-10) }) { Text("−10 %") }
            OutlinedButton(onClick = { viewModel.changeStateOfCharge(10) }) { Text("+10 %") }
        }
        Button(onClick = onBack) { Text("Назад до введення") }
    }
}
