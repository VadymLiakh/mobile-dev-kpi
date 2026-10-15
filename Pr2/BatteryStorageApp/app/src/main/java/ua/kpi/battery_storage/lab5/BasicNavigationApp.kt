package ua.kpi.battery_storage.lab5

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ua.kpi.battery_storage.ui.theme.BatteryStorageTheme

/**
 * Базовий рівень ПЗ 5: головне меню з заголовком і двома кнопками,
 * що ведуть на екрани-заглушки. Навігацію виконують NavController і NavHost.
 */
@Composable
fun BasicNavigationApp() {
    // NavController керує стеком екранів цього графа
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Basic.Menu.route) {
        composable(Screen.Basic.Menu.route) {
            BasicMainMenu(
                onStatusClick = { navController.navigate(Screen.Basic.Status.route) },
                onOperatorClick = { navController.navigate(Screen.Basic.Operator.route) },
            )
        }
        composable(Screen.Basic.Status.route) {
            StubScreen(
                title = "Стан батареї",
                text = "Екран-заглушка. Тут відображатимуться рівень заряду, потужність " +
                    "і температура накопичувача енергії.",
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Basic.Operator.route) {
            StubScreen(
                title = "Оператор системи",
                text = "Екран-заглушка. Тут відображатиметься інформація про чергового оператора BESS.",
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/** Головне меню: заголовок і дві кнопки, розміщені вертикально у Column. */
@Composable
private fun BasicMainMenu(onStatusClick: () -> Unit, onOperatorClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "🔋 Battery Storage",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Головне меню",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onStatusClick, modifier = Modifier.fillMaxWidth()) {
            Text("Стан батареї")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onOperatorClick, modifier = Modifier.fillMaxWidth()) {
            Text("Оператор системи")
        }
    }
}

/** Екран-заглушка з заголовком, поясненням і кнопкою повернення (popBackStack). */
@Composable
private fun StubScreen(title: String, text: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = text, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = onBack) { Text("Назад до меню") }
    }
}

@Preview(showBackground = true)
@Composable
private fun BasicMainMenuPreview() {
    BatteryStorageTheme { BasicMainMenu({}, {}) }
}
