package ua.kpi.battery_storage.lab5.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val ROUTE_LOGIN = "auth_login"
private const val ROUTE_REGISTER = "auth_register"
private const val ROUTE_WELCOME = "auth_welcome/{login}"

/**
 * Домашнє завдання ПЗ 5: екрани авторизації та реєстрації оператора накопичувача.
 * Зареєстровані користувачі зберігаються в пам'яті (mutableStateMapOf);
 * для перевірки створено демо-обліковий запис operator / battery10.
 */
@Composable
fun AuthApp() {
    val navController = rememberNavController()
    val users = remember { mutableStateMapOf("operator" to "battery10") }

    NavHost(navController = navController, startDestination = ROUTE_LOGIN) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                users = users,
                onLoggedIn = { login -> navController.navigate("auth_welcome/$login") },
                onRegisterClick = { navController.navigate(ROUTE_REGISTER) },
            )
        }
        composable(ROUTE_REGISTER) {
            RegisterScreen(
                users = users,
                onRegistered = { login, password ->
                    users[login] = password
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(ROUTE_WELCOME) { entry ->
            WelcomeScreen(
                login = entry.arguments?.getString("login").orEmpty(),
                onLogout = { navController.logout() },
            )
        }
    }
}

/** Вихід: повертаємося на екран входу й очищаємо стек від вітального екрана. */
private fun NavHostController.logout() {
    navigate(ROUTE_LOGIN) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
}

/** Спільний контейнер екранів: прокрутка та обмеження ширини для планшетів. */
@Composable
private fun AuthContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

/** Екран входу: логін, пароль, кнопка входу і перехід до реєстрації. */
@Composable
private fun LoginScreen(
    users: Map<String, String>,
    onLoggedIn: (String) -> Unit,
    onRegisterClick: () -> Unit,
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    AuthContainer {
        AuthHeader("Вхід до системи", "Battery Storage — панель оператора")
        LoginField(
            value = login,
            onValueChange = { login = it.trim(); message = null },
            error = if (login.isEmpty()) null else AuthValidator.validateLogin(login),
        )
        PasswordField(
            value = password,
            onValueChange = { password = it; message = null },
            error = null,
            hint = "Демо-обліковий запис: operator / battery10",
        )
        AuthMessage(message, isError = true)
        AuthButton(
            text = "Увійти",
            enabled = AuthValidator.canSubmitLogin(login, password),
            onClick = {
                message = AuthValidator.authenticate(users, login, password)
                if (message == null) onLoggedIn(login)
            },
        )
        TextButton(onClick = onRegisterClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Немає облікового запису? Зареєструватися")
        }
    }
}

/** Екран реєстрації: логін, пароль і підтвердження з перевіркою унікальності логіна. */
@Composable
private fun RegisterScreen(
    users: Map<String, String>,
    onRegistered: (login: String, password: String) -> Unit,
    onBack: () -> Unit,
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    // Після першої спроби показуємо помилки навіть для незаповнених полів
    fun errorFor(value: String, check: () -> String?) = if (value.isEmpty() && !submitted) null else check()

    val loginError = errorFor(login) {
        AuthValidator.validateLogin(login) ?: if (login in users) "Логін «$login» уже зайнятий" else null
    }
    val passwordError = errorFor(password) { AuthValidator.validatePassword(password) }
    val confirmationError = errorFor(confirmation) { AuthValidator.validateConfirmation(password, confirmation) }

    AuthContainer {
        AuthHeader("Реєстрація", "Створіть обліковий запис оператора")
        LoginField(login, { login = it.trim() }, loginError)
        PasswordField(password, { password = it }, passwordError)
        PasswordField(
            value = confirmation,
            onValueChange = { confirmation = it },
            error = confirmationError,
            label = "Повторіть пароль",
            hint = "Має збігатися з паролем",
        )
        AuthButton(
            text = "Зареєструватися",
            enabled = true,
            onClick = {
                submitted = true
                if (AuthValidator.validateRegistration(users, login, password, confirmation) == null) {
                    onRegistered(login, password)
                }
            },
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Назад до входу")
        }
    }
}

/** Екран після успішного входу. */
@Composable
private fun WelcomeScreen(login: String, onLogout: () -> Unit) {
    AuthContainer {
        AuthHeader("Вітаємо, $login!", "Ви успішно увійшли до панелі керування накопичувачем")
        Text(
            "Тепер доступні моніторинг заряду та керування режимами роботи BESS.",
            style = MaterialTheme.typography.bodyMedium,
        )
        AuthButton(text = "Вийти", enabled = true, onClick = onLogout)
    }
}
