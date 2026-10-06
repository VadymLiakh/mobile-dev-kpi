// Кореневий файл збірки: лише оголошує плагіни, які використовують модулі проєкту
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
