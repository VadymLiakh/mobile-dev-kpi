package ua.kpi.battery_storage.lab5.home

/**
 * Бізнес-логіка авторизації та реєстрації, винесена з UI в окремі функції.
 * Кожна функція повертає текст помилки або null, якщо значення коректне.
 */
object AuthValidator {
    const val MIN_LOGIN_LENGTH = 4
    const val MIN_PASSWORD_LENGTH = 6

    private val LoginPattern = Regex("^[A-Za-z0-9_.]+$")

    fun validateLogin(login: String): String? = when {
        login.isBlank() -> "Введіть логін"
        login.length < MIN_LOGIN_LENGTH -> "Логін має містити щонайменше $MIN_LOGIN_LENGTH символи"
        !LoginPattern.matches(login) -> "Лише латинські літери, цифри, «_» та «.»"
        else -> null
    }

    fun validatePassword(password: String): String? = when {
        password.isBlank() -> "Введіть пароль"
        password.length < MIN_PASSWORD_LENGTH -> "Пароль має містити щонайменше $MIN_PASSWORD_LENGTH символів"
        password.none(Char::isDigit) -> "Пароль має містити хоча б одну цифру"
        else -> null
    }

    fun validateConfirmation(password: String, confirmation: String): String? = when {
        confirmation.isBlank() -> "Повторіть пароль"
        confirmation != password -> "Паролі не збігаються"
        else -> null
    }

    /** Кнопка входу активна лише тоді, коли обидва поля заповнені. */
    fun canSubmitLogin(login: String, password: String): Boolean =
        login.isNotBlank() && password.isNotBlank()

    /** Перевірка облікових даних у сховищі користувачів. */
    fun authenticate(users: Map<String, String>, login: String, password: String): String? = when {
        login !in users -> "Користувача «$login» не знайдено — зареєструйтеся"
        users[login] != password -> "Невірний пароль"
        else -> null
    }

    /** Повна перевірка форми реєстрації, включно з унікальністю логіна. */
    fun validateRegistration(
        users: Map<String, String>,
        login: String,
        password: String,
        confirmation: String,
    ): String? = validateLogin(login)
        ?: (if (login in users) "Логін «$login» уже зайнятий" else null)
        ?: validatePassword(password)
        ?: validateConfirmation(password, confirmation)
}
