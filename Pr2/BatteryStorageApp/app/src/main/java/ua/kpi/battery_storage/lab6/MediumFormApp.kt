package ua.kpi.battery_storage.lab6

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ua.kpi.battery_storage.lab6.domain.FormRules

/**
 * Середній рівень ПЗ 6: форма реєстрації накопичувача з email оператора, паролем,
 * ємністю і потужністю. Перевірка формату email і мінімальної довжини пароля
 * виконується «на льоту» при кожній зміні State; підказки — через supportingText.
 */
@Composable
fun MediumFormApp() {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var capacity by rememberSaveable { mutableStateOf("") }
    var power by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }

    // Поле перевіряється, щойно в ньому з'явився текст, або після спроби відправлення
    fun check(value: String, rule: () -> String?) = if (value.isEmpty() && !submitted) null else rule()

    val nameError = check(name) { FormRules.required(name, "Назва") }
    val emailError = check(email) { FormRules.email(email) }
    val passwordError = check(password) { FormRules.minPasswordLength(password) }
    val capacityError = check(capacity) { FormRules.numberInRange(capacity, "Ємність", 10.0..10_000.0, "кВт·год") }
    val powerError = check(power) { FormRules.numberInRange(power, "Потужність", 1.0..5_000.0, "кВт") }

    val allFilled = listOf(name, email, password, capacity, power).all { it.isNotBlank() }
    val hasErrors = listOf(nameError, emailError, passwordError, capacityError, powerError).any { it != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Реєстрація накопичувача",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        // Група 1: дані системи
        Text("Система", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        FormField(name, { name = it }, "Назва системи", nameError, "Як система відображатиметься в журналі")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(
                capacity, { capacity = it }, "Ємність, кВт·год", capacityError, "10–10 000",
                keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f),
            )
            FormField(
                power, { power = it }, "Потужність, кВт", powerError, "1–5 000",
                keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f),
            )
        }

        // Група 2: обліковий запис оператора
        Text("Оператор", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        FormField(
            email, { email = it }, "Email оператора", emailError, "Формат: name@domain.ua",
            keyboardType = KeyboardType.Email,
        )
        FormField(
            password, { password = it }, "Пароль", passwordError,
            "Мінімум ${FormRules.MIN_PASSWORD_LENGTH} символів (${password.length}/${FormRules.MIN_PASSWORD_LENGTH})",
            keyboardType = KeyboardType.Password, isPassword = true,
        )

        Button(
            onClick = {
                submitted = true
                result = if (allFilled && !hasErrors) "Систему «${name.trim()}» зареєстровано на $email" else null
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text("Зареєструвати") }

        result?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

/** Поле форми з підказкою (helper text), яку замінює текст помилки. */
@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    helper: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = { Text(error ?: helper) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        modifier = modifier,
    )
}
