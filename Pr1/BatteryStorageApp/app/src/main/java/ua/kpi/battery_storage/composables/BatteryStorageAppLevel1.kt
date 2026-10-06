package ua.kpi.battery_storage.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Базовий рівень: заголовок, текст, кнопка оновлення та простий колір фону.
 */
@Composable
fun BatteryStorageAppLevel1(modifier: Modifier = Modifier) {
    // Лічильник натискань — найпростіша «інформація», яку оновлює кнопка
    var updates by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9)) // світло-зелений фон
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Hello Battery Storage",
            fontSize = 28.sp,
            color = Color(0xFF1B5E20), // темно-зелений текст
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Система накопичення енергії",
            fontSize = 18.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (updates == 0) "Дані ще не оновлювались" else "Оновлень даних: $updates",
            fontSize = 16.sp,
            color = Color.DarkGray,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { updates++ }) {
            Text("Оновити дані")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BatteryStorageAppLevel1Preview() {
    BatteryStorageAppLevel1()
}
