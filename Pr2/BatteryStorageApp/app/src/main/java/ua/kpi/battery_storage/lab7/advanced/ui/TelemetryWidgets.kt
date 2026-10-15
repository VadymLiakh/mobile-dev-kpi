package ua.kpi.battery_storage.lab7.advanced.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.kpi.battery_storage.lab7.advanced.domain.BatteryInsights
import ua.kpi.battery_storage.lab7.advanced.domain.HealthStatus

/**
 * VIEW (шар MVVM). Повторно використовувані віджети відображення даних:
 * отримують готові значення і не містять бізнес-логіки.
 */

/** Плитка одного показника: назва, значення та необов'язковий підпис. */
@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier, caption: String? = null, valueColor: Color = Color.Unspecified) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
            if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Банер загального стану системи з кольором залежно від статусу та переліком попереджень. */
@Composable
fun StatusBanner(insights: BatteryInsights) {
    val (container, content) = when (insights.status) {
        HealthStatus.NORMAL -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        HealthStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        HealthStatus.CRITICAL -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Стан: ${insights.status.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (insights.alerts.isEmpty()) Text("Усі параметри в межах норми")
            insights.alerts.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/**
 * Лінійний графік рівня заряду за історією показань, намальований на Canvas.
 * Вісь Y — 0…100 %, точки розподілені рівномірно по осі X.
 */
@Composable
fun SocHistoryChart(values: List<Double>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Історія заряду (останні ${values.size})", style = MaterialTheme.typography.labelMedium)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 8.dp),
            ) {
                // Горизонтальні лінії сітки: 0, 50, 100 %
                listOf(0f, 0.5f, 1f).forEach { level ->
                    val y = size.height * (1 - level)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                if (values.size < 2) return@Canvas
                val stepX = size.width / (values.size - 1)
                val path = Path()
                values.forEachIndexed { i, v ->
                    val point = Offset(i * stepX, size.height * (1 - (v / 100.0).toFloat()))
                    if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx()))
                drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset((values.size - 1) * stepX, size.height * (1 - (values.last() / 100.0).toFloat())))
            }
        }
    }
}

/** Повноекранний стан першого завантаження. */
@Composable
fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text("Підключення до контролера BMS…", modifier = Modifier.padding(top = 12.dp))
    }
}

/** Стан помилки з кнопкою повтору. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Помилка отримання даних", fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onRetry) { Text("Повторити") }
        }
    }
}
