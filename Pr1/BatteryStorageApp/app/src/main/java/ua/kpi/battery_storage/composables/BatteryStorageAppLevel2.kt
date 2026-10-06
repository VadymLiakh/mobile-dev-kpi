package ua.kpi.battery_storage.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.kpi.battery_storage.model.BatteryStatus

/**
 * Середній рівень: змінні дані у текстових полях, випадкове оновлення
 * для симуляції динаміки, компонування через Column і Row.
 */
@Composable
fun BatteryStorageAppLevel2(modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf(BatteryStatus.Initial) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE0F2F1)) // світло-бірюзовий фон
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Система накопичення енергії",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
        )
        Text(
            text = "Ємність: ${BatteryStatus.CAPACITY_KWH} кВт·год",
            fontSize = 16.sp,
            color = Color.DarkGray,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ValueField("Рівень заряду", "${status.stateOfCharge} %", Modifier.weight(1f))
            ValueField("Потужність", formatPower(status.powerKw), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ValueField(
                "Накопичено",
                "${status.storedEnergyKwh.formatOneDecimal()} кВт·год",
                Modifier.weight(1f),
            )
            ValueField(
                "Температура",
                "${status.temperatureC.formatOneDecimal()} °C",
                Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Режим: ${status.mode.title}",
            fontSize = 18.sp,
            color = Color(0xFF004D40),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { status = BatteryStatus.random() }) {
            Text("Оновити показники")
        }
    }
}

/** Поле з підписом і значенням показника. */
@Composable
private fun ValueField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BatteryStorageAppLevel2Preview() {
    BatteryStorageAppLevel2()
}
