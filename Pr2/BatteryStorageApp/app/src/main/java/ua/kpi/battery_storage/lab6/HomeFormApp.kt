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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Домашнє завдання ПЗ 6: заявка на доступ до моніторингу накопичувача.
 * Два поля (ім'я та email), перевірка на непорожні значення і підказки під полями.
 * Взаємодія забезпечується через Compose State без складної бізнес-логіки.
 */
@Composable
fun HomeFormApp() {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var sentTo by rememberSaveable { mutableStateOf<String?>(null) }

    val nameEmpty = submitted && name.isBlank()
    val emailEmpty = submitted && email.isBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Заявка на доступ до BESS",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Заповніть дані, щоб отримувати звіти про стан накопичувача.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; sentTo = null },
            label = { Text("Ім'я") },
            placeholder = { Text("Наприклад, Вадим") },
            isError = nameEmpty,
            supportingText = {
                Text(if (nameEmpty) "Введіть ім'я" else "Ім'я відображатиметься у звітах")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; sentTo = null },
            label = { Text("Електронна пошта") },
            placeholder = { Text("name@example.com") },
            isError = emailEmpty,
            supportingText = {
                Text(if (emailEmpty) "Введіть електронну пошту" else "Сюди надходитимуть звіти")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                submitted = true
                if (name.isNotBlank() && email.isNotBlank()) sentTo = email.trim()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Надіслати заявку") }

        sentTo?.let { address ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Дякуємо, ${name.trim()}! Підтвердження надіслано на $address.",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
