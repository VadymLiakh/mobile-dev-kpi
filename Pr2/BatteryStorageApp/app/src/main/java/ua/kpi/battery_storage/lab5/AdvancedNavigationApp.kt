package ua.kpi.battery_storage.lab5

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ua.kpi.battery_storage.lab5.advanced.AdvancedMainMenuScreen
import ua.kpi.battery_storage.lab5.advanced.ChargeSettingsScreen
import ua.kpi.battery_storage.lab5.advanced.DashboardScreen
import ua.kpi.battery_storage.lab5.advanced.ModuleDetailsScreen
import ua.kpi.battery_storage.lab5.advanced.OperatorProfileScreen
import ua.kpi.battery_storage.lab5.advanced.rememberBatteryControlState

/**
 * Просунутий рівень ПЗ 5. Scaffold з TopAppBar показує назву поточного екрана
 * і кнопку «Назад»; усі екрани отримують спільний тримач стану з бізнес-логікою.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedNavigationApp() {
    val navController = rememberNavController()
    val state = rememberBatteryControlState()

    // Поточний запис стеку потрібен для заголовка та видимості кнопки «Назад»
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = Screen.advancedByRoute(backStackEntry?.destination?.route)
    val canGoBack = currentScreen != Screen.Advanced.Menu

    Scaffold(
        // Відступи від системних панелей уже враховано зовнішнім Scaffold у MainActivity
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(currentScreen.title) },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Advanced.Menu.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Screen.Advanced.Menu.route) {
                AdvancedMainMenuScreen(
                    state = state,
                    onDashboard = { navController.navigate(Screen.Advanced.Dashboard.route) },
                    onSettings = { navController.navigate(Screen.Advanced.Settings.route) },
                    onProfile = { navController.navigate(Screen.Advanced.Profile.route) },
                )
            }
            composable(Screen.Advanced.Dashboard.route) {
                DashboardScreen(
                    state = state,
                    onModuleClick = { id -> navController.navigate(Screen.Advanced.Module.createRoute(id)) },
                )
            }
            composable(
                route = Screen.Advanced.Module.route,
                arguments = listOf(navArgument(Screen.ARG_MODULE_ID) { type = NavType.IntType }),
            ) { entry ->
                ModuleDetailsScreen(state, moduleId = entry.arguments?.getInt(Screen.ARG_MODULE_ID) ?: -1)
            }
            composable(Screen.Advanced.Settings.route) {
                ChargeSettingsScreen(state, onSaved = { navController.popBackStack() })
            }
            composable(Screen.Advanced.Profile.route) {
                OperatorProfileScreen(state)
            }
        }
    }
}
