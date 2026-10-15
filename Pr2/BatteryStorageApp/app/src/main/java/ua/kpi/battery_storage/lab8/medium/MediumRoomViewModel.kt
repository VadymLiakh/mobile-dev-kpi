package ua.kpi.battery_storage.lab8.medium

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel середнього рівня ПЗ 8. Рядок пошуку — це StateFlow; flatMapLatest
 * перемикає підписку на новий запит до Room при кожній зміні тексту,
 * тому список оновлюється «реактивно» як під час пошуку, так і після змін у базі.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediumRoomViewModel(private val repository: MediumRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val modules: StateFlow<List<ModuleWithReadings>> = _query
        .flatMapLatest { repository.observeModules(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lastOrigin: StateFlow<DataOrigin?> = repository.lastOrigin

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        sync(force = false)
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun sync(force: Boolean) {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            val origin = repository.sync(force)
            _message.value = "Дані взято з джерела: ${origin.title}"
            _syncing.value = false
        }
    }

    /** Перевірка полів діалогу; повертає текст помилки або null. */
    fun validateModule(name: String, capacity: String): String? {
        val value = capacity.replace(',', '.').toDoubleOrNull()
        return when {
            name.isBlank() -> "Введіть назву модуля"
            value == null || value <= 0 -> "Ємність має бути додатним числом"
            value > 2_000 -> "Ємність одного модуля — до 2000 кВт·год"
            else -> null
        }
    }

    /** Додає (module == null) або оновлює модуль. Результат повідомляється через [onResult]. */
    fun saveModule(module: BatteryModuleEntity?, name: String, capacity: String, onResult: (String?) -> Unit) {
        validateModule(name, capacity)?.let { onResult(it); return }
        val capacityKwh = capacity.replace(',', '.').toDouble()
        viewModelScope.launch {
            val error = if (module == null) {
                repository.addModule(name, capacityKwh)
            } else {
                repository.updateModule(module.copy(name = name, capacityKwh = capacityKwh))
            }
            if (error == null) _message.value = if (module == null) "Модуль «$name» додано" else "Модуль «$name» оновлено"
            onResult(error)
        }
    }

    fun deleteModule(module: BatteryModuleEntity) {
        viewModelScope.launch {
            repository.deleteModule(module)
            _message.value = "Модуль «${module.name}» та його показання видалено"
        }
    }

    /** Ручне показання (імітація вимірювання оператором на місці). */
    fun addLocalReading(moduleId: Long) {
        viewModelScope.launch {
            repository.addLocalReading(moduleId, Random.nextInt(5, 101), 20.0 + Random.nextDouble() * 15.0)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                MediumRoomViewModel(MediumRepository(MediumDatabase.getInstance(app).moduleDao(), RemoteTelemetryApi()))
            }
        }
    }
}
