package ua.kpi.battery_storage.lab6

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Базовий рівень ПЗ 6: форма з двома полями TextField і кнопкою підтвердження.
 * Після натискання перевіряється, що поля не порожні; помилка з'являється під полем.
 */
@Composable
fun BasicFormApp() {
    var name by rememberSaveable { mutableStateOf("") }
    var capacity by rememberSaveable { mutableStateOf("") }
    // Помилки показуються лише після першої спроби відправити форму
    var submitted by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }

    val nameError = submitted && name.isBlank()
    val capacityError = submitted && capacity.isBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Реєстрація накопичувача",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        TextField(
            value = name,
            onValueChange = { name = it; result = null },
            label = { Text("Назва системи") },
            isError = nameError,
            supportingText = { if (nameError) Text("Поле не може бути порожнім") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TextField(
            value = capacity,
            onValueChange = { capacity = it; result = null },
            label = { Text("Ємність, кВт·год") },
            isError = capacityError,
            supportingText = { if (capacityError) Text("Поле не може бути порожнім") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                submitted = true
                if (name.isNotBlank() && capacity.isNotBlank()) {
                    result = "Систему «${name.trim()}» ємністю ${capacity.trim()} кВт·год зареєстровано"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Підтвердити") }

        result?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
