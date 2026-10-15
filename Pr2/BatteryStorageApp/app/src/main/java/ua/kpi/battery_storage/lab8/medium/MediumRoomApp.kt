package ua.kpi.battery_storage.lab8.medium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.battery_storage.lab8.formatDateTime
import ua.kpi.battery_storage.ui.components.chargeColor
import ua.kpi.battery_storage.ui.components.formatOneDecimal

/** Стан діалогу: null — закритий; module == null — додавання нового модуля. */
private data class ModuleDialog(val module: BatteryModuleEntity?)

/**
 * Середній рівень ПЗ 8: модулі та їх показання (дві пов'язані таблиці),
 * пошук, додавання, редагування, видалення і синхронізація з «сервером».
 */
@Composable
fun MediumRoomApp(viewModel: MediumRoomViewModel = viewModel(factory = MediumRoomViewModel.Factory)) {
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<ModuleDialog?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Пошук модуля за назвою") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { dialog = ModuleDialog(null) }, modifier = Modifier.weight(1f)) { Text("Додати модуль") }
            OutlinedButton(onClick = { viewModel.sync(force = true) }, enabled = !syncing, modifier = Modifier.weight(1f)) {
                Text("Синхронізувати")
            }
        }
        if (syncing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }

        if (modules.isEmpty() && !syncing) {
            Text(if (query.isBlank()) "База порожня" else "За запитом «$query» нічого не знайдено")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(modules, key = { it.module.id }) { item ->
                ModuleItem(
                    item = item,
                    onAddReading = { viewModel.addLocalReading(item.module.id) },
                    onEdit = { dialog = ModuleDialog(item.module) },
                    onDelete = { viewModel.deleteModule(item.module) },
                )
            }
        }
    }

    dialog?.let { state ->
        ModuleEditDialog(
            module = state.module,
            onDismiss = { dialog = null },
            onSave = { name, capacity, showError ->
                viewModel.saveModule(state.module, name, capacity) { error ->
                    if (error == null) dialog = null else showError(error)
                }
            },
        )
    }
}

/** Картка модуля з останнім показанням і кнопками дій. */
@Composable
private fun ModuleItem(
    item: ModuleWithReadings,
    onAddReading: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val latest = item.latest
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.module.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${item.module.capacityKwh.formatOneDecimal()} кВт·год · показань: ${item.readings.size}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (latest != null) {
                    Text(
                        "SoC ${latest.stateOfCharge} % · ${latest.temperatureC.formatOneDecimal()} °C",
                        color = chargeColor(latest.stateOfCharge),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${formatDateTime(latest.recordedAt)} · ${if (latest.source == RemoteTelemetryApi.SOURCE_REMOTE) "сервер" else "вручну"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("Показань ще немає", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onAddReading) { Icon(Icons.Filled.Add, contentDescription = "Додати показання") }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Редагувати") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Видалити", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Діалог додавання / редагування модуля з валідацією. */
@Composable
private fun ModuleEditDialog(
    module: BatteryModuleEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, capacity: String, showError: (String) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf(module?.name.orEmpty()) }
    var capacity by remember { mutableStateOf(module?.capacityKwh?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (module == null) "Новий модуль" else "Редагування модуля") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it; error = null }, label = { Text("Назва") }, singleLine = true)
                OutlinedTextField(
                    capacity, { capacity = it; error = null },
                    label = { Text("Ємність, кВт·год") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, capacity) { error = it } }) { Text("Зберегти") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}
