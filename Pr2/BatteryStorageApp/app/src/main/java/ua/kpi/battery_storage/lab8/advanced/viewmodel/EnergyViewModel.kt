package ua.kpi.battery_storage.lab8.advanced.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.kpi.battery_storage.lab8.advanced.data.EnergyDatabase
import ua.kpi.battery_storage.lab8.advanced.data.ModeFilter
import ua.kpi.battery_storage.lab8.advanced.data.RecordSort
import ua.kpi.battery_storage.lab8.advanced.data.RecordWithUnit
import ua.kpi.battery_storage.lab8.advanced.data.RemoteEnergySource
import ua.kpi.battery_storage.lab8.advanced.data.UnitSummary
import ua.kpi.battery_storage.lab8.advanced.repository.DataResult
import ua.kpi.battery_storage.lab8.advanced.repository.EnergyRepository
import ua.kpi.battery_storage.lab8.advanced.repository.FleetStatistics
import ua.kpi.battery_storage.lab8.advanced.repository.SiteOverview

/** Поточні параметри фільтрації та сортування. */
data class RecordFilter(
    val siteId: Long? = null,
    val unitId: Long? = null,
    val mode: ModeFilter = ModeFilter.ALL,
    val sort: RecordSort = RecordSort.TIME_DESC,
)

/** Стан синхронізації для індикатора та повідомлень. */
data class SyncState(val running: Boolean = false, val message: String? = null, val isError: Boolean = false)

/**
 * ViewModel просунутого рівня ПЗ 8. Один StateFlow фільтра керує трьома незалежними
 * потоками з репозиторію: зведеннями установок, статистикою та списком записів.
 * flatMapLatest скасовує попередній запит до Room, щойно користувач змінює фільтр.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EnergyViewModel(private val repository: EnergyRepository) : ViewModel() {

    private val _filter = MutableStateFlow(RecordFilter())
    val filter: StateFlow<RecordFilter> = _filter.asStateFlow()

    private val _sync = MutableStateFlow(SyncState())
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    val sites: StateFlow<List<SiteOverview>> = repository.observeSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Зведення залежать лише від майданчика, тому зміна сортування їх не перезапитує
    private val siteId = _filter.map { it.siteId }.distinctUntilChanged()

    val summaries: StateFlow<List<UnitSummary>> = siteId
        .flatMapLatest { repository.observeUnitSummaries(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val statistics: StateFlow<FleetStatistics?> = siteId
        .flatMapLatest { repository.observeFleetStatistics(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val records: StateFlow<List<RecordWithUnit>> = _filter
        .flatMapLatest { f -> repository.observeRecords(f.siteId, f.unitId, f.mode, f.sort) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    fun selectSite(siteId: Long?) = _filter.update { it.copy(siteId = siteId, unitId = null) }

    /** Повторне натискання на ту саму установку знімає фільтр. */
    fun selectUnit(unitId: Long) = _filter.update { it.copy(unitId = if (it.unitId == unitId) null else unitId) }

    fun selectMode(mode: ModeFilter) = _filter.update { it.copy(mode = mode) }

    fun selectSort(sort: RecordSort) = _filter.update { it.copy(sort = sort) }

    fun sync(force: Boolean) {
        if (_sync.value.running) return
        viewModelScope.launch {
            _sync.value = SyncState(running = true, message = "Завантаження телеметрії…")
            _sync.value = when (val result = repository.sync(force)) {
                is DataResult.Success -> SyncState(message = "Синхронізовано: ${result.data.title}")
                is DataResult.Error -> SyncState(message = result.message, isError = true)
            }
        }
    }

    fun purgeOld() {
        viewModelScope.launch {
            val deleted = repository.purgeOlderThan(hours = 6)
            _sync.value = SyncState(message = "Видалено записів старших за 6 год: $deleted")
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                EnergyViewModel(EnergyRepository(EnergyDatabase.getInstance(app).energyDao(), RemoteEnergySource()))
            }
        }
    }
}
