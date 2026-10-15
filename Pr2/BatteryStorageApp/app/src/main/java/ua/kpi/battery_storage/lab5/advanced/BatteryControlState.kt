package ua.kpi.battery_storage.lab5.advanced

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ua.kpi.battery_storage.ui.components.formatOneDecimal
import kotlin.math.roundToInt
import kotlin.random.Random

/** Роль користувача визначає, чи може він змінювати налаштування заряду. */
enum class OperatorRole(val title: String, val canEditSettings: Boolean) {
    OBSERVER("Спостерігач", false),
    OPERATOR("Оператор", true),
    ENGINEER("Інженер", true),
}

/** Стан користувача (оператора), що працює із системою. */
data class OperatorProfile(
    val name: String = "Лях В. О.",
    val role: OperatorRole = OperatorRole.ENGINEER,
    val onDuty: Boolean = true,
)

/** Збережені налаштування: робочий діапазон рівня заряду, %. */
data class ChargeSettings(val minSoc: Int = 15, val maxSoc: Int = 90) {
    companion object {
        /** Мінімальна ширина робочого діапазону, щоб накопичувач мав корисну ємність. */
        const val MIN_WINDOW = 20

        /** Перевірка діапазону; повертає текст помилки або null, якщо все гаразд. */
        fun validate(minSoc: Int, maxSoc: Int): String? = when {
            minSoc < 5 -> "Мінімальний рівень не може бути нижчим за 5 % (глибокий розряд шкодить батареї)"
            maxSoc > 100 -> "Максимальний рівень не може перевищувати 100 %"
            maxSoc - minSoc < MIN_WINDOW -> "Робочий діапазон має становити щонайменше $MIN_WINDOW %"
            else -> null
        }
    }
}

/** Стан одного батарейного модуля (4 модулі × 125 кВт·год = 500 кВт·год). */
data class BatteryModuleState(
    val id: Int,
    val soc: Int,
    val temperatureC: Double,
    val powerKw: Int,
) {
    val name: String get() = "Модуль $id"
    val storedEnergyKwh: Double get() = MODULE_CAPACITY_KWH * soc / 100.0

    companion object {
        const val MODULE_CAPACITY_KWH = 125
        const val MAX_SAFE_TEMPERATURE = 35.0
    }
}

/** Попередження, сформоване бізнес-логікою. */
data class BatteryAlert(val text: String, val critical: Boolean)

/**
 * Тримач стану просунутого рівня ПЗ 5: профіль оператора, збережені налаштування
 * та телеметрія модулів. Містить бізнес-логіку (права доступу, перевірку налаштувань,
 * формування попереджень), тому екрани лише відображають готові дані.
 */
class BatteryControlState(
    private val random: Random = Random.Default,
) {
    var profile by mutableStateOf(OperatorProfile())
        private set
    var settings by mutableStateOf(ChargeSettings())
        private set
    var modules by mutableStateOf(List(4) { randomModule(it + 1) })
        private set

    /** Середній рівень заряду системи, %. */
    val averageSoc: Int get() = modules.map { it.soc }.average().roundToInt()

    /** Сумарна потужність обміну з мережею, кВт. */
    val totalPowerKw: Int get() = modules.sumOf { it.powerKw }

    val storedEnergyKwh: Double get() = modules.sumOf { it.storedEnergyKwh }

    /** Змінювати налаштування може лише користувач з відповідною роллю, який перебуває на зміні. */
    val canEditSettings: Boolean get() = profile.onDuty && profile.role.canEditSettings

    /** Попередження, що залежать від телеметрії, налаштувань і стану користувача. */
    val alerts: List<BatteryAlert>
        get() = buildList {
            if (!profile.onDuty) add(BatteryAlert("Оператор не на зміні — керування заблоковано", false))
            modules.forEach { module ->
                if (module.temperatureC > BatteryModuleState.MAX_SAFE_TEMPERATURE) {
                    add(BatteryAlert("${module.name}: перегрів ${module.temperatureC.formatOneDecimal()} °C", true))
                }
                if (module.soc < settings.minSoc) {
                    add(BatteryAlert("${module.name}: заряд ${module.soc} % нижче мінімуму", true))
                } else if (module.soc > settings.maxSoc) {
                    add(BatteryAlert("${module.name}: заряд ${module.soc} % вище максимуму", false))
                }
            }
        }

    fun module(id: Int): BatteryModuleState? = modules.firstOrNull { it.id == id }

    /** Симулює отримання нових показників з усіх модулів. */
    fun refreshTelemetry() {
        modules = modules.map { randomModule(it.id) }
    }

    /** Зберігає налаштування, якщо користувач має права і значення коректні. */
    fun applySettings(minSoc: Int, maxSoc: Int): String? {
        if (!canEditSettings) return "Недостатньо прав: потрібна роль оператора або інженера на зміні"
        ChargeSettings.validate(minSoc, maxSoc)?.let { return it }
        settings = ChargeSettings(minSoc, maxSoc)
        return null
    }

    fun updateProfile(newProfile: OperatorProfile) {
        profile = newProfile.copy(name = newProfile.name.trim().ifEmpty { profile.name })
    }

    private fun randomModule(id: Int) = BatteryModuleState(
        id = id,
        soc = random.nextInt(5, 101),
        temperatureC = 22.0 + random.nextDouble() * 16.0,
        powerKw = random.nextInt(-60, 61),
    )
}

/** Створює тримач стану, спільний для всіх екранів навігаційного графа. */
@Composable
fun rememberBatteryControlState(): BatteryControlState = remember { BatteryControlState() }
