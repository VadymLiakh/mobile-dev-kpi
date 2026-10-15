package ua.kpi.battery_storage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Контейнерні та нейтральні кольори задано явно: інакше Material 3 підставляє
// стандартні фіолетові значення для карток, фону та індикаторів
private val DarkColorScheme = darkColorScheme(
    primary = EnergyGreen80,
    primaryContainer = EnergyGreenContainer80,
    onPrimaryContainer = OnEnergyGreenContainer80,
    secondary = Teal80,
    secondaryContainer = TealContainer80,
    onSecondaryContainer = OnTealContainer80,
    tertiary = Amber80,
    tertiaryContainer = AmberContainer80,
    onTertiaryContainer = OnAmberContainer80,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

private val LightColorScheme = lightColorScheme(
    primary = EnergyGreen40,
    primaryContainer = EnergyGreenContainer40,
    onPrimaryContainer = OnEnergyGreenContainer40,
    secondary = Teal40,
    secondaryContainer = TealContainer40,
    onSecondaryContainer = OnTealContainer40,
    tertiary = Amber40,
    tertiaryContainer = AmberContainer40,
    onTertiaryContainer = OnAmberContainer40,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

/**
 * Тема застосунку. Динамічні кольори Android 12+ свідомо не використовуються,
 * щоб інтерфейс мав однаковий «енергетичний» стиль на всіх пристроях.
 */
@Composable
fun BatteryStorageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
