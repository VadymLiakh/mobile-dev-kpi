package ua.kpi.battery_storage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ua.kpi.battery_storage.lab5.AdvancedNavigationApp
import ua.kpi.battery_storage.lab5.BasicNavigationApp
import ua.kpi.battery_storage.lab5.MediumNavigationApp
import ua.kpi.battery_storage.lab5.home.AuthApp
import ua.kpi.battery_storage.lab6.AdvancedFormApp
import ua.kpi.battery_storage.lab6.BasicFormApp
import ua.kpi.battery_storage.lab6.HomeFormApp
import ua.kpi.battery_storage.lab6.MediumFormApp
import ua.kpi.battery_storage.lab7.advanced.ui.AdvancedMvvmApp
import ua.kpi.battery_storage.lab7.basic.BasicMvvmApp
import ua.kpi.battery_storage.lab7.home.HomeMvvmApp
import ua.kpi.battery_storage.lab7.medium.MediumMvvmApp
import ua.kpi.battery_storage.lab8.advanced.ui.AdvancedRoomApp
import ua.kpi.battery_storage.lab8.basic.BasicRoomApp
import ua.kpi.battery_storage.lab8.home.HomeRoomApp
import ua.kpi.battery_storage.lab8.medium.MediumRoomApp
import ua.kpi.battery_storage.ui.components.DisplayModeSelector
import ua.kpi.battery_storage.ui.theme.BatteryStorageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BatteryStorageTheme {
                // Обране заняття та режим зберігаються і після повороту екрана
                var lab by rememberSaveable { mutableStateOf(Lab.NAVIGATION) }
                var displayFor by rememberSaveable { mutableStateOf(DisplayFor.BASIC_LEVEL) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        DisplayModeSelector(
                            lab = lab,
                            displayFor = displayFor,
                            onLabChange = { lab = it },
                            onDisplayForChange = { displayFor = it },
                        )
                        HorizontalDivider()

                        Box(modifier = Modifier.weight(1f)) {
                            // key: при зміні режиму вкладені NavHost отримують новий NavController
                            key(lab, displayFor) {
                                LabContent(lab, displayFor)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Вибирає реалізацію для пари «заняття — рівень». */
@Composable
private fun LabContent(lab: Lab, displayFor: DisplayFor) {
    when (lab) {
        Lab.NAVIGATION -> when (displayFor) {
            DisplayFor.HOME_TASK -> AuthApp()
            DisplayFor.BASIC_LEVEL -> BasicNavigationApp()
            DisplayFor.MIDDLE_LEVEL -> MediumNavigationApp()
            DisplayFor.ADVANCED_LEVEL -> AdvancedNavigationApp()
        }
        Lab.FORMS -> when (displayFor) {
            DisplayFor.HOME_TASK -> HomeFormApp()
            DisplayFor.BASIC_LEVEL -> BasicFormApp()
            DisplayFor.MIDDLE_LEVEL -> MediumFormApp()
            DisplayFor.ADVANCED_LEVEL -> AdvancedFormApp()
        }
        Lab.MVVM -> when (displayFor) {
            DisplayFor.HOME_TASK -> HomeMvvmApp()
            DisplayFor.BASIC_LEVEL -> BasicMvvmApp()
            DisplayFor.MIDDLE_LEVEL -> MediumMvvmApp()
            DisplayFor.ADVANCED_LEVEL -> AdvancedMvvmApp()
        }
        Lab.ROOM -> when (displayFor) {
            DisplayFor.HOME_TASK -> HomeRoomApp()
            DisplayFor.BASIC_LEVEL -> BasicRoomApp()
            DisplayFor.MIDDLE_LEVEL -> MediumRoomApp()
            DisplayFor.ADVANCED_LEVEL -> AdvancedRoomApp()
        }
    }
}
