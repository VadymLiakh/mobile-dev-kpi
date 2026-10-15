package ua.kpi.battery_storage

/** Практичні заняття, що входять до практичної роботи № 2. */
enum class Lab(val shortTitle: String, val topic: String) {
    NAVIGATION("ПЗ 5", "Головне меню та навігація (Navigation Component)"),
    FORMS("ПЗ 6", "Форми введення даних і валідація"),
    MVVM("ПЗ 7", "Архітектура MVVM: ViewModel, LiveData, StateFlow"),
    ROOM("ПЗ 8", "Локальне сховище даних: Room Database"),
}

/** Режими відображення кожного заняття: домашнє завдання та три рівні складності. */
enum class DisplayFor(val title: String) {
    HOME_TASK("ДЗ"),
    BASIC_LEVEL("Базовий"),
    MIDDLE_LEVEL("Середній"),
    ADVANCED_LEVEL("Просунутий"),
}
