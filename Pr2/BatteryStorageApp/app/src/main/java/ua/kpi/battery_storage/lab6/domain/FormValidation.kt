package ua.kpi.battery_storage.lab6.domain

import kotlinx.coroutines.delay

/**
 * Правила перевірки полів форм ПЗ 6. Функції не залежать від UI,
 * тому їх можна покрити модульними тестами. Кожна повертає текст помилки або null.
 */
object FormRules {
    private val EmailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    const val MIN_PASSWORD_LENGTH = 8

    fun required(value: String, field: String): String? =
        if (value.isBlank()) "Поле «$field» не може бути порожнім" else null

    fun email(value: String): String? = required(value, "Email")
        ?: if (!EmailRegex.matches(value.trim())) "Невірний формат email (приклад: name@bess.ua)" else null

    fun minPasswordLength(value: String): String? = required(value, "Пароль")
        ?: if (value.length < MIN_PASSWORD_LENGTH) "Мінімум $MIN_PASSWORD_LENGTH символів (зараз ${value.length})" else null

    /** Число в заданому діапазоні; [unit] додається до тексту помилки. */
    fun numberInRange(value: String, field: String, range: ClosedFloatingPointRange<Double>, unit: String): String? {
        required(value, field)?.let { return it }
        val number = value.replace(',', '.').toDoubleOrNull() ?: return "«$field» має бути числом"
        return if (number !in range) {
            "«$field»: допустимо ${range.start.toInt()}–${range.endInclusive.toInt()} $unit"
        } else {
            null
        }
    }

    fun parseNumber(value: String): Double? = value.replace(',', '.').toDoubleOrNull()
}

/** Тип хімії акумуляторів визначає допустимий C-rate (відношення потужності до ємності). */
enum class BatteryChemistry(val title: String, val maxCRate: Double) {
    LFP("LiFePO₄ (LFP)", 1.0),
    NMC("NMC", 2.0),
}

/** Оцінка складності пароля за кількістю виконаних критеріїв. */
enum class PasswordStrength(val title: String, val score: Int) {
    WEAK("Слабкий", 1),
    MEDIUM("Середній", 2),
    STRONG("Надійний", 3),
}

/** Комплексна бізнес-логіка просунутої форми реєстрації накопичувача. */
object BatteryFormValidator {
    const val MIN_CAPACITY_KWH = 10.0
    const val MAX_CAPACITY_KWH = 10_000.0

    /** Критерії складності пароля: назва — перевірка. */
    val passwordCriteria: List<Pair<String, (String) -> Boolean>> = listOf(
        "не менше ${FormRules.MIN_PASSWORD_LENGTH} символів" to { it.length >= FormRules.MIN_PASSWORD_LENGTH },
        "велика літера" to { p -> p.any(Char::isUpperCase) },
        "мала літера" to { p -> p.any(Char::isLowerCase) },
        "цифра" to { p -> p.any(Char::isDigit) },
        "спецсимвол (!@#…)" to { p -> p.any { !it.isLetterOrDigit() && !it.isWhitespace() } },
    )

    fun passwordStrength(password: String): PasswordStrength {
        val passed = passwordCriteria.count { (_, check) -> check(password) }
        return when {
            passed == passwordCriteria.size -> PasswordStrength.STRONG
            passed >= 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    /** Надійним вважається лише пароль, що відповідає всім критеріям. */
    fun validatePassword(password: String): String? {
        FormRules.required(password, "Пароль")?.let { return it }
        val missing = passwordCriteria.filterNot { (_, check) -> check(password) }.map { it.first }
        return if (missing.isEmpty()) null else "Додайте: ${missing.joinToString(", ")}"
    }

    fun validateStationName(name: String): String? = FormRules.required(name, "Назва")
        ?: if (name.trim().length !in 3..40) "Назва має містити від 3 до 40 символів" else null

    fun validateCapacity(capacity: String): String? =
        FormRules.numberInRange(capacity, "Ємність", MIN_CAPACITY_KWH..MAX_CAPACITY_KWH, "кВт·год")

    /**
     * Потужність інвертора не може перевищувати ємність × max C-rate хімії.
     * Наприклад, для LFP ємністю 500 кВт·год максимум — 500 кВт.
     */
    fun validatePower(power: String, capacity: String, chemistry: BatteryChemistry): String? {
        FormRules.required(power, "Потужність")?.let { return it }
        val powerKw = FormRules.parseNumber(power) ?: return "«Потужність» має бути числом"
        if (powerKw <= 0) return "Потужність має бути більшою за 0"
        val capacityKwh = FormRules.parseNumber(capacity) ?: return null
        val maxPower = capacityKwh * chemistry.maxCRate
        return if (powerKw > maxPower) {
            "Для ${chemistry.title} максимум ${maxPower.toInt()} кВт (${chemistry.maxCRate}C)"
        } else {
            null
        }
    }
}

/**
 * Імітація віддаленого реєстру операторів. Перевірка унікальності email —
 * асинхронна операція з затримкою, як запит до сервера.
 */
class OperatorRegistry(initialEmails: Set<String> = setOf("admin@bess.ua", "operator@bess.ua")) {
    private val emails = initialEmails.map { it.lowercase() }.toMutableSet()

    suspend fun isEmailTaken(email: String): Boolean {
        delay(NETWORK_DELAY_MS)
        return email.trim().lowercase() in emails
    }

    suspend fun register(email: String) {
        delay(NETWORK_DELAY_MS)
        emails += email.trim().lowercase()
    }

    companion object {
        const val NETWORK_DELAY_MS = 700L
    }
}
