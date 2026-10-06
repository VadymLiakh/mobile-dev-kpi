package ua.kpi.battery_storage.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ua.kpi.battery_storage.model.BatteryMode
import ua.kpi.battery_storage.model.BatteryStatus
import ua.kpi.battery_storage.ui.theme.BatteryStorageTheme
import kotlin.math.abs

private const val ANIMATION_MS = 900

/**
 * Просунутий рівень: анімована зміна значень, керування станом через State
 * та стилізація кнопки, тексту й фону через MaterialTheme.
 */
@Composable
fun BatteryStorageAppLevel3(modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf(BatteryStatus.Initial) }

    // Кожне значення плавно переходить до нового цільового стану
    val animatedSoc by animateIntAsState(
        targetValue = status.stateOfCharge,
        animationSpec = tween(ANIMATION_MS),
        label = "SocAnimation",
    )
    val animatedPower by animateFloatAsState(
        targetValue = status.powerKw.toFloat(),
        animationSpec = tween(ANIMATION_MS),
        label = "PowerAnimation",
    )
    val animatedEnergy by animateFloatAsState(
        targetValue = status.storedEnergyKwh.toFloat(),
        animationSpec = tween(ANIMATION_MS),
        label = "EnergyAnimation",
    )
    val indicatorColor by animateColorAsState(
        targetValue = chargeColor(status.stateOfCharge),
        animationSpec = tween(ANIMATION_MS),
        label = "ChargeColorAnimation",
    )

    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Система накопичення енергії",
            style = typography.headlineMedium,
            color = colors.primary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        BatteryIndicator(
            fraction = animatedSoc / 100f,
            fillColor = indicatorColor,
            outlineColor = colors.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$animatedSoc %",
            style = typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
        )

        Spacer(modifier = Modifier.height(20.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricRow("Потужність", formatPower(animatedPower))
                MetricRow(
                    "Накопичено",
                    "${animatedEnergy.toDouble().formatOneDecimal()} / ${BatteryStatus.CAPACITY_KWH} кВт·год",
                )
                MetricRow("Температура", "${status.temperatureC.formatOneDecimal()} °C")
                MetricRow("Режим", status.mode.title, highlight = status.mode != BatteryMode.IDLE)

                Spacer(modifier = Modifier.height(12.dp))

                // Завантаження інвертора відносно максимальної потужності
                Text(
                    text = "Завантаження інвертора",
                    style = typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { abs(animatedPower) / BatteryStatus.MAX_POWER_KW },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.secondary,
                    trackColor = colors.secondaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { status = BatteryStatus.random() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Оновити показники", style = typography.labelLarge)
        }
    }
}

/** Рядок «назва — значення» всередині картки. */
@Composable
private fun MetricRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BatteryStorageAppLevel3Preview() {
    BatteryStorageTheme {
        BatteryStorageAppLevel3()
    }
}
