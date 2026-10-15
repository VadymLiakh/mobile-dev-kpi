package ua.kpi.battery_storage.lab7.medium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel середнього рівня ПЗ 7. Три вхідні StateFlow (ємність, заряд, потужність)
 * об'єднуються оператором combine у похідні потоки: помилки валідації та розрахунок.
 * Будь-яка зміна введення автоматично перераховує всі залежні стани.
 */
class MediumBatteryViewModel : ViewModel() {
    private val _capacity = MutableStateFlow("500")
    private val _soc = MutableStateFlow("60")
    private val _power = MutableStateFlow("120")

    val capacity: StateFlow<String> = _capacity.asStateFlow()
    val soc: StateFlow<String> = _soc.asStateFlow()
    val power: StateFlow<String> = _power.asStateFlow()

    /** Помилки валідації для кожного поля (порожня мапа - помилок немає). */
    val errors: StateFlow<Map<CalcField, String>> =
        combine(_capacity, _soc, _power) { c, s, p -> BatteryCalculator.validate(c, s, p) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Розрахунок похідних величин або null, якщо введення некоректне. */
    val calculation: StateFlow<BatteryCalculation?> =
        combine(_capacity, _soc, _power) { c, s, p -> BatteryCalculator.calculate(c, s, p) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BatteryCalculator.calculate("500", "60", "120"))

    /** Погодинний прогноз залежить від розрахунку - ланцюжок похідних станів. */
    val projection: StateFlow<List<Pair<Int, Double>>> =
        calculation.map { it?.hourlyProjection().orEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onCapacityChange(value: String) { _capacity.value = value.sanitize() }
    fun onSocChange(value: String) { _soc.value = value.sanitize() }
    fun onPowerChange(value: String) { _power.value = value.sanitize(allowMinus = true) }

    /** Швидке перемикання напряму потоку енергії (заряд / розряд). */
    fun reversePower() {
        val p = _power.value.toDoubleOrNull() ?: return
        _power.value = (-p).toInt().toString()
    }

    private fun String.sanitize(allowMinus: Boolean = false): String =
        replace(',', '.').filterIndexed { i, ch -> ch.isDigit() || ch == '.' || (allowMinus && ch == '-' && i == 0) }
}
