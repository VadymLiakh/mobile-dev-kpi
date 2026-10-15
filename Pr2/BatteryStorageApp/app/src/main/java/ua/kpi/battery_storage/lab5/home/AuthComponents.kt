package ua.kpi.battery_storage.lab5.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Заголовок екрана авторизації: емодзі системи, назва та підзаголовок. */
@Composable
fun AuthHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("🔋", style = MaterialTheme.typography.displaySmall)
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Поле логіна. Помилка показується лише після того, як користувач почав вводити,
 * щоб порожня форма не була одразу «червоною».
 */
@Composable
fun LoginField(value: String, onValueChange: (String) -> Unit, error: String?) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Логін") },
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
        isError = error != null,
        supportingText = { HintText(error, hint = "Латинські літери та цифри") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Поле пароля з кнопкою «Показати / Сховати». */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    label: String = "Пароль",
    hint: String = "Щонайменше 6 символів, одна цифра",
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) { Text(if (visible) "Сховати" else "Показати") }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = error != null,
        supportingText = { HintText(error, hint) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Підказка під полем: показує помилку червоним або нейтральну підказку. */
@Composable
fun HintText(error: String?, hint: String) {
    Text(
        text = error ?: hint,
        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Основна кнопка форми; стан enabled обчислюється бізнес-логікою. */
@Composable
fun AuthButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) { Text(text) }
}

/** Повідомлення про результат дії (помилка входу або успіх реєстрації). */
@Composable
fun AuthMessage(text: String?, isError: Boolean) {
    if (text == null) return
    Text(
        text = text,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
