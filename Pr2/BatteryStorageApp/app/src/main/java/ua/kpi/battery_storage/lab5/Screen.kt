package ua.kpi.battery_storage.lab5

/**
 * Маршрути навігаційних графів ПЗ 5. Кожен рівень має власний набір маршрутів,
 * тому sealed-ієрархія гарантує, що в NavHost потрапляють лише відомі екрани.
 */
sealed class Screen(val route: String) {

    /** Базовий рівень: головне меню і два екрани-заглушки. */
    sealed class Basic(route: String) : Screen(route) {
        data object Menu : Basic("basic_menu")
        data object Status : Basic("basic_status")
        data object Operator : Basic("basic_operator")
    }

    /** Середній рівень: додано екран налаштувань із формою та передачу аргументів. */
    sealed class Medium(route: String) : Screen(route) {
        data object Menu : Medium("medium_menu")
        data object Operator : Medium("medium_operator")

        /** Екран стану отримує рівень заряду, потужність і межу заряду як аргументи маршруту. */
        data object Status : Medium("medium_status/{$ARG_SOC}/{$ARG_POWER}/{$ARG_LIMIT}") {
            fun createRoute(soc: Int, powerKw: Int, limit: Int) = "medium_status/$soc/$powerKw/$limit"
        }

        /** Екран налаштувань отримує поточну межу заряду, щоб заповнити поле форми. */
        data object Settings : Medium("medium_settings/{$ARG_LIMIT}") {
            fun createRoute(limit: Int) = "medium_settings/$limit"
        }
    }

    /** Просунутий рівень: панель стану, деталі модуля, налаштування, профіль. */
    sealed class Advanced(route: String, val title: String) : Screen(route) {
        data object Menu : Advanced("advanced_menu", "Головне меню")
        data object Dashboard : Advanced("advanced_dashboard", "Стан системи")
        data object Settings : Advanced("advanced_settings", "Налаштування заряду")
        data object Profile : Advanced("advanced_profile", "Профіль оператора")

        data object Module : Advanced("advanced_module/{$ARG_MODULE_ID}", "Батарейний модуль") {
            fun createRoute(moduleId: Int) = "advanced_module/$moduleId"
        }
    }

    companion object {
        const val ARG_SOC = "soc"
        const val ARG_POWER = "powerKw"
        const val ARG_LIMIT = "limit"
        const val ARG_MODULE_ID = "moduleId"

        /** Ключ SavedStateHandle, через який екран налаштувань повертає результат у меню. */
        const val RESULT_CHARGE_LIMIT = "charge_limit"

        /** Шукає екран просунутого рівня за шаблоном маршруту (для заголовка TopAppBar). */
        fun advancedByRoute(route: String?): Advanced = listOf(
            Advanced.Menu, Advanced.Dashboard, Advanced.Settings, Advanced.Profile, Advanced.Module,
        ).firstOrNull { it.route == route } ?: Advanced.Menu
    }
}
