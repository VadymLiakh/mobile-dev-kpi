package ua.kpi.battery_storage.lab8.advanced.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.battery_storage.lab8.advanced.data.ModeFilter
import ua.kpi.battery_storage.lab8.advanced.data.RecordSort
import ua.kpi.battery_storage.lab8.advanced.data.RecordWithUnit
import ua.kpi.battery_storage.lab8.advanced.viewmodel.EnergyViewModel

/**
 * Просунутий рівень ПЗ 8: аналітика парку накопичувачів.
 * Вузький екран — одна прокручувана колонка; широкий — дві панелі:
 * ліворуч майданчики, статистика й установки, праворуч фільтри та записи.
 */
@Composable
fun AdvancedRoomApp(viewModel: EnergyViewModel = viewModel(factory = EnergyViewModel.Factory)) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sync by viewModel.sync.collectAsStateWithLifecycle()
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val stats by viewModel.statistics.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()

    // Блоки інтерфейсу оголошено один раз і скомпоновано по-різному залежно від ширини
    val actions: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.sync(force = false) }, enabled = !sync.running, modifier = Modifier.weight(1f)) {
                    Text("Синхронізувати")
                }
                OutlinedButton(onClick = viewModel::purgeOld, enabled = !sync.running, modifier = Modifier.weight(1f)) {
                    Text("Очистити > 6 год")
                }
            }
            if (sync.running) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            sync.message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sync.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    val siteFilter: @Composable () -> Unit = {
        val options = listOf<Long?>(null) + sites.map { it.site.id }
        ChipRow(
            label = "Майданчик",
            options = options,
            selected = filter.siteId,
            title = { id ->
                sites.firstOrNull { it.site.id == id }?.let { "${it.site.name} (${it.unitCount})" } ?: "Усі"
            },
            onSelect = viewModel::selectSite,
        )
    }
    val overview: @Composable (vertical: Boolean) -> Unit = { vertical ->
        stats?.let { FleetStatsCard(it) }
        Text("Установки (натисніть, щоб відфільтрувати записи)", style = MaterialTheme.typography.labelMedium)
        val cards: @Composable () -> Unit = {
            summaries.forEach { s ->
                UnitSummaryCard(
                    summary = s,
                    selected = filter.unitId == s.unitId,
                    onClick = { viewModel.selectUnit(s.unitId) },
                    modifier = if (vertical) Modifier.fillMaxWidth() else Modifier,
                )
            }
        }
        if (vertical) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { cards() }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { cards() }
        }
    }
    val recordFilters: @Composable () -> Unit = {
        ChipRow("Режим", ModeFilter.entries, filter.mode, { it.title }, viewModel::selectMode)
        ChipRow("Сортування", RecordSort.entries, filter.sort, { it.title }, viewModel::selectSort)
        Text("Записи: ${records.size}", style = MaterialTheme.typography.titleSmall)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    actions()
                    siteFilter()
                    overview(true)
                }
                LazyColumn(modifier = Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { recordFilters() } }
                    recordItems(records)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { actions() }
                item { siteFilter() }
                item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { overview(false) } }
                item { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { recordFilters() } }
                recordItems(records)
            }
        }
    }
}

/** Записи телеметрії як елементи LazyColumn (спільно для обох макетів). */
private fun LazyListScope.recordItems(records: List<RecordWithUnit>) {
    if (records.isEmpty()) item { Text("Немає записів для обраних фільтрів") }
    items(records, key = { it.record.id }) { RecordRow(it) }
}
