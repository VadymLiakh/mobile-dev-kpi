package ua.kpi.battery_storage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ua.kpi.battery_storage.composables.BatteryStorageAppLevel1
import ua.kpi.battery_storage.composables.BatteryStorageAppLevel2
import ua.kpi.battery_storage.composables.BatteryStorageAppLevel3
import ua.kpi.battery_storage.composables.BatteryStorageHomeTask
import ua.kpi.battery_storage.composables.DisplayModeSelector
import ua.kpi.battery_storage.ui.theme.BatteryStorageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BatteryStorageTheme {
                // Обраний режим зберігається і після повороту екрана
                var displayFor by rememberSaveable { mutableStateOf(DisplayFor.HOME_TASK) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        DisplayModeSelector(
                            selected = displayFor,
                            onSelectedChange = { displayFor = it },
                        )
                        HorizontalDivider()

                        val screenModifier = Modifier.weight(1f)
                        when (displayFor) {
                            DisplayFor.HOME_TASK -> BatteryStorageHomeTask(screenModifier)
                            DisplayFor.BASIC_LEVEL -> BatteryStorageAppLevel1(screenModifier)
                            DisplayFor.MIDDLE_LEVEL -> BatteryStorageAppLevel2(screenModifier)
                            DisplayFor.ADVANCED_LEVEL -> BatteryStorageAppLevel3(screenModifier)
                        }
                    }
                }
            }
        }
    }
}
