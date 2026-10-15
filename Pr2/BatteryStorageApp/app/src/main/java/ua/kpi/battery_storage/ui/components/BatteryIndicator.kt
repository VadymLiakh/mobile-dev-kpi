package ua.kpi.battery_storage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ua.kpi.battery_storage.ui.theme.ChargeHigh
import ua.kpi.battery_storage.ui.theme.ChargeLow
import ua.kpi.battery_storage.ui.theme.ChargeMedium

/** Колір індикатора залежно від рівня заряду, %. */
fun chargeColor(stateOfCharge: Int): Color = when {
    stateOfCharge >= 60 -> ChargeHigh
    stateOfCharge >= 25 -> ChargeMedium
    else -> ChargeLow
}

/**
 * Горизонтальний значок акумулятора, заповнений відповідно до [fraction] (0..1).
 */
@Composable
fun BatteryIndicator(
    fraction: Float,
    fillColor: Color,
    outlineColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 180.dp, height = 80.dp)) {
        val stroke = 4.dp.toPx()
        val capWidth = 10.dp.toPx()
        val bodyWidth = size.width - capWidth
        val corner = CornerRadius(12.dp.toPx())

        // Корпус
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(bodyWidth - stroke, size.height - stroke),
            cornerRadius = corner,
            style = Stroke(width = stroke),
        )
        // Контакт («плюс») батареї
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(bodyWidth, size.height * 0.3f),
            size = Size(capWidth, size.height * 0.4f),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        // Заповнення пропорційно до заряду
        val inset = stroke * 2
        val innerWidth = bodyWidth - inset * 2
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(inset, inset),
            size = Size(innerWidth * fraction.coerceIn(0f, 1f), size.height - inset * 2),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )
    }
}
