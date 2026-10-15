package ua.kpi.battery_storage.lab7.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ua.kpi.battery_storage.model.BatteryMode

/** Незмінний знімок стану екрана моніторингу. */
data class MonitorUiState(
    val stateOfCharge: Double = 50.0,
    val mode: BatteryMode = BatteryMode.CHARGING,
    val powerKw: Float = 100f,
    val running: Boolean = false,
    val simulatedMinutes: Int = 0,
    val log: List<String> = emptyList(),
) {
    val storedKwh: Double get() = BatterySimulator.CAPACITY_KWH * stateOfCharge / 100.0
}

/**
 * VIEWMODEL домашнього завдання ПЗ 7. Стан зберігається у Compose State (mutableStateOf),
 * тому Compose відстежує його напряму, без collectAsState. Симуляція виконується
 * в корутині viewModelScope: кожні 0,5 с моделюються 5 хвилин роботи накопичувача.
 */
class BatteryMonitorViewModel : ViewModel() {
    var uiState by mutableStateOf(MonitorUiState())
        private set

    private var simulation: Job? = null

    fun selectMode(mode: BatteryMode) {
        uiState = uiState.copy(mode = mode).withLog("Режим: ${mode.title}")
    }

    fun setPower(powerKw: Float) {
        uiState = uiState.copy(powerKw = powerKw)
    }

    fun toggleSimulation() = if (uiState.running) stop("Симуляцію зупинено") else start()

    fun reset() {
        simulation?.cancel()
        uiState = MonitorUiState()
    }

    private fun start() {
        uiState = uiState.copy(running = true).withLog("Симуляцію запущено")
        simulation = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val state = uiState
                val soc = BatterySimulator.step(state.stateOfCharge, state.mode, state.powerKw.toDouble(), STEP_HOURS)
                uiState = state.copy(stateOfCharge = soc, simulatedMinutes = state.simulatedMinutes + STEP_MINUTES)
                if (BatterySimulator.limitReached(soc, state.mode)) {
                    stop(if (state.mode == BatteryMode.CHARGING) "Батарею повністю заряджено" else "Батарею повністю розряджено")
                }
            }
        }
    }

    private fun stop(reason: String) {
        simulation?.cancel()
        uiState = uiState.copy(running = false).withLog(reason)
    }

    /** Додає подію в журнал (зберігаються останні 6 подій). */
    private fun MonitorUiState.withLog(event: String): MonitorUiState {
        val time = "%02d:%02d".format(simulatedMinutes / 60, simulatedMinutes % 60)
        return copy(log = (listOf("[$time] $event") + log).take(6))
    }

    private companion object {
        const val TICK_MS = 500L
        const val STEP_MINUTES = 5
        const val STEP_HOURS = STEP_MINUTES / 60.0
    }
}
