package ua.kpi.battery_storage.lab8.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.kpi.battery_storage.lab8.formatDateTime
import ua.kpi.battery_storage.ui.components.chargeColor

/** ViewModel базового рівня ПЗ 8: список записів із Room і логіка додавання. */
class BasicRoomViewModel(private val repository: SocRepository) : ViewModel() {
    /** Flow з Room перетворено на StateFlow, за яким спостерігає UI. */
    val records: StateFlow<List<SocRecord>> = repository.records
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var input by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun onInputChange(value: String) {
        input = value.filter(Char::isDigit).take(3)
        error = null
    }

    fun save() {
        val soc = input.toIntOrNull()
        error = when {
            soc == null -> "Введіть рівень заряду"
            soc !in 0..100 -> "Допустимо 0–100 %"
            else -> null
        }
        if (soc == null || error != null) return
        viewModelScope.launch { repository.add(soc) }
        input = ""
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                BasicRoomViewModel(SocRepository(BasicDatabase.getInstance(app).socRecordDao()))
            }
        }
    }
}

/** Базовий рівень ПЗ 8: введення рівня заряду і список збережених значень. */
@Composable
fun BasicRoomApp(viewModel: BasicRoomViewModel = viewModel(factory = BasicRoomViewModel.Factory)) {
    val records by viewModel.records.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Журнал рівня заряду", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = viewModel.input,
                onValueChange = viewModel::onInputChange,
                label = { Text("SoC, %") },
                isError = viewModel.error != null,
                supportingText = { Text(viewModel.error ?: "0–100") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = viewModel::save, modifier = Modifier.padding(top = 8.dp)) { Text("Зберегти") }
        }
        Text("Записів у базі: ${records.size}", style = MaterialTheme.typography.labelLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(records, key = { it.id }) { record ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("#${record.id}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 12.dp))
                        Text(
                            "${record.stateOfCharge} %",
                            fontWeight = FontWeight.Bold,
                            color = chargeColor(record.stateOfCharge),
                            modifier = Modifier.weight(1f),
                        )
                        Text(formatDateTime(record.recordedAt), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
