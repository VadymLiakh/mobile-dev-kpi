package ua.kpi.battery_storage.lab7.advanced.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ua.kpi.battery_storage.lab7.advanced.domain.BatteryAnalyzer
import ua.kpi.battery_storage.lab7.advanced.domain.BatteryInsights
import ua.kpi.battery_storage.lab7.advanced.domain.SimulatedTelemetrySource
import ua.kpi.battery_storage.lab7.advanced.domain.TelemetryReading
import ua.kpi.battery_storage.lab7.advanced.domain.TelemetrySource

/**
 * Стан екрана. UI не вирішує, що показувати: він лише відображає один із трьох станів,
 * які формує ViewModel (завантаження, успіх, помилка).
 */
sealed interface TelemetryUiState {
    data object Loading : TelemetryUiState

    data class Success(
        val latest: TelemetryReading,
        val history: List<TelemetryReading>,
        val insights: BatteryInsights,
        val refreshing: Boolean = false,
    ) : TelemetryUiState

    /** Помилка; [previous] дозволяє показати останні відомі дані разом із повідомленням. */
    data class Error(val message: String, val previous: Success?) : TelemetryUiState
}

/**
 * VIEWMODEL (шар MVVM між View і Model). Отримує дані з TelemetrySource у корутинах
 * viewModelScope, передає їх аналізатору та публікує результат як StateFlow.
 * Корутини автоматично скасовуються, коли ViewModel знищується.
 */
class TelemetryViewModel(
    private val source: TelemetrySource,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TelemetryUiState>(TelemetryUiState.Loading)
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    private val _autoRefresh = MutableStateFlow(false)
    val autoRefresh: StateFlow<Boolean> = _autoRefresh.asStateFlow()

    private val history = ArrayDeque<TelemetryReading>()
    private var refreshJob: Job? = null
    private var autoRefreshJob: Job? = null

    init {
        refresh()
    }

    /** Запитує нове показання; повторні натискання під час запиту ігноруються. */
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch { loadOnce() }
    }

    /** Вмикає / вимикає періодичне оновлення кожні [AUTO_REFRESH_MS] мс. */
    fun toggleAutoRefresh() {
        val enable = !_autoRefresh.value
        _autoRefresh.value = enable
        autoRefreshJob?.cancel()
        if (enable) {
            autoRefreshJob = viewModelScope.launch {
                while (isActive) {
                    // refresh() гарантує, що одночасно виконується лише один запит
                    refresh()
                    refreshJob?.join()
                    delay(AUTO_REFRESH_MS)
                }
            }
        }
    }

    private suspend fun loadOnce() {
        val previous = currentSuccess()
        // Якщо дані вже є — показуємо їх з позначкою оновлення замість повноекранного завантаження
        _uiState.value = previous?.copy(refreshing = true) ?: TelemetryUiState.Loading
        try {
            val reading = source.fetch()
            history.addLast(reading)
            while (history.size > HISTORY_SIZE) history.removeFirst()
            val snapshot = history.toList()
            _uiState.value = TelemetryUiState.Success(reading, snapshot, BatteryAnalyzer.analyze(snapshot))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { TelemetryUiState.Error(e.message ?: "Невідома помилка", previous) }
        }
    }

    private fun currentSuccess(): TelemetryUiState.Success? = when (val state = _uiState.value) {
        is TelemetryUiState.Success -> state
        is TelemetryUiState.Error -> state.previous
        TelemetryUiState.Loading -> null
    }

    companion object {
        const val HISTORY_SIZE = 20
        const val AUTO_REFRESH_MS = 2_500L

        /** Фабрика передає залежність (джерело даних) у конструктор ViewModel. */
        val Factory = viewModelFactory {
            initializer { TelemetryViewModel(SimulatedTelemetrySource()) }
        }
    }
}
