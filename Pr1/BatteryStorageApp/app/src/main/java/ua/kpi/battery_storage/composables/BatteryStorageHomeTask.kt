package ua.kpi.battery_storage.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import ua.kpi.battery_storage.model.BatteryStatus
import ua.kpi.battery_storage.ui.theme.BatteryStorageTheme

/**
 * Домашнє завдання: початковий екран із заголовком, основним показником (рівень заряду),
 * другим показником (потужність і режим) та кнопкою оновлення.
 */
@Composable
fun BatteryStorageHomeTask(modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf(BatteryStatus.Initial) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Заголовок із символом, що відображає тип системи
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🔋", style = MaterialTheme.typography.headlineMedium) // 🔋
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Battery Storage",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Основний показник
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Рівень заряду",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "${status.stateOfCharge} %",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Другий показник: потужність обміну з мережею та режим роботи
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmallInfoCard("Потужність", formatPower(status.powerKw), Modifier.weight(1f))
            SmallInfoCard("Режим", status.mode.title, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { status = BatteryStatus.random() }) {
            Text("Оновити значення")
        }
    }
}

@Composable
private fun SmallInfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BatteryStorageHomeTaskPreview() {
    BatteryStorageTheme {
        BatteryStorageHomeTask()
    }
}
