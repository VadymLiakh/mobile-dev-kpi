package ua.kpi.battery_storage.lab8.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import ua.kpi.battery_storage.ui.components.formatOneDecimal
import ua.kpi.battery_storage.ui.components.formatPower

/**
 * ViewModel домашнього завдання ПЗ 8: стан форми (Compose State) та список
 * вимірювань з Room (StateFlow). Одна форма використовується і для додавання,
 * і для редагування — залежно від того, чи обрано запис ([editing]).
 */
class MeasurementViewModel(private val repository: MeasurementRepository) : ViewModel() {
    val measurements: StateFlow<List<MeasurementEntity>> = repository.measurements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var soc by mutableStateOf("")
    var power by mutableStateOf("")
    var temperature by mutableStateOf("")
    var note by mutableStateOf("")
    var errors by mutableStateOf<Map<MeasurementField, String>>(emptyMap())
        private set
    var editing by mutableStateOf<MeasurementEntity?>(null)
        private set

    fun save() {
        errors = MeasurementValidator.validate(soc, power, temperature)
        if (errors.isNotEmpty()) return
        val base = editing ?: MeasurementEntity(stateOfCharge = 0, powerKw = 0, temperatureC = 0.0, note = "", recordedAt = System.currentTimeMillis())
        val measurement = base.copy(
            stateOfCharge = soc.toInt(),
            powerKw = power.toInt(),
            temperatureC = temperature.replace(',', '.').toDouble(),
            note = note.trim(),
        )
        viewModelScope.launch { repository.save(measurement) }
        clearForm()
    }

    /** Заповнює форму значеннями запису для редагування. */
    fun edit(measurement: MeasurementEntity) {
        editing = measurement
        soc = measurement.stateOfCharge.toString()
        power = measurement.powerKw.toString()
        temperature = measurement.temperatureC.toString()
        note = measurement.note
        errors = emptyMap()
    }

    fun delete(measurement: MeasurementEntity) {
        if (editing?.id == measurement.id) clearForm()
        viewModelScope.launch { repository.delete(measurement) }
    }

    fun clearForm() {
        editing = null
        soc = ""; power = ""; temperature = ""; note = ""
        errors = emptyMap()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                MeasurementViewModel(MeasurementRepository(MeasurementDatabase.getInstance(app).measurementDao()))
            }
        }
    }
}

/** Домашнє завдання ПЗ 8: журнал вимірювань BESS — форма та таблиця з CRUD. */
@Composable
fun HomeRoomApp(viewModel: MeasurementViewModel = viewModel(factory = MeasurementViewModel.Factory)) {
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MeasurementForm(viewModel)
        Text("Журнал (${measurements.size})", style = MaterialTheme.typography.titleMedium)
        TableHeader()
        LazyColumn {
            items(measurements, key = { it.id }) { m ->
                MeasurementRow(
                    measurement = m,
                    highlighted = viewModel.editing?.id == m.id,
                    onEdit = { viewModel.edit(m) },
                    onDelete = { viewModel.delete(m) },
                )
                HorizontalDivider()
            }
        }
    }
}

/** Форма введення / редагування з підказками та помилками валідації. */
@Composable
private fun MeasurementForm(viewModel: MeasurementViewModel) {
    val errors = viewModel.errors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (viewModel.editing == null) "Нове вимірювання" else "Редагування запису #${viewModel.editing?.id}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumberInput(viewModel.soc, { viewModel.soc = it }, "SoC, %", errors[MeasurementField.SOC], Modifier.weight(1f))
            NumberInput(viewModel.power, { viewModel.power = it }, "P, кВт", errors[MeasurementField.POWER], Modifier.weight(1f))
            NumberInput(viewModel.temperature, { viewModel.temperature = it }, "t, °C", errors[MeasurementField.TEMPERATURE], Modifier.weight(1f))
        }
        OutlinedTextField(
            value = viewModel.note,
            onValueChange = { viewModel.note = it.take(40) },
            label = { Text("Примітка (необов'язково)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::save, modifier = Modifier.weight(1f)) {
                Text(if (viewModel.editing == null) "Додати" else "Оновити")
            }
            OutlinedButton(onClick = viewModel::clearForm, modifier = Modifier.weight(1f)) { Text("Очистити") }
        }
    }
}

@Composable
private fun NumberInput(value: String, onChange: (String) -> Unit, label: String, error: String?, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
}

/** Заголовок таблиці. */
@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text("Час / примітка", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text("SoC", Modifier.width(48.dp), style = MaterialTheme.typography.labelMedium)
        Text("P", Modifier.width(72.dp), style = MaterialTheme.typography.labelMedium)
        Text("t°", Modifier.width(56.dp), style = MaterialTheme.typography.labelMedium)
        Text("", Modifier.width(80.dp))
    }
}

/** Рядок таблиці з кнопками редагування та видалення. */
@Composable
private fun MeasurementRow(measurement: MeasurementEntity, highlighted: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(formatDateTime(measurement.recordedAt), style = MaterialTheme.typography.bodySmall)
            if (measurement.note.isNotEmpty()) {
                Text(measurement.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            "${measurement.stateOfCharge}%",
            Modifier.width(48.dp),
            color = chargeColor(measurement.stateOfCharge),
            fontWeight = FontWeight.Bold,
        )
        Text(formatPower(measurement.powerKw), Modifier.width(72.dp), style = MaterialTheme.typography.bodySmall)
        Text("${measurement.temperatureC.formatOneDecimal()}°", Modifier.width(56.dp), style = MaterialTheme.typography.bodySmall)
        IconButton(onClick = onEdit, modifier = Modifier.width(40.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Редагувати") }
        IconButton(onClick = onDelete, modifier = Modifier.width(40.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Видалити", tint = MaterialTheme.colorScheme.error)
        }
    }
}
