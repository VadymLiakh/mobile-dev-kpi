package ua.kpi.battery_storage.lab6.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ua.kpi.battery_storage.lab6.domain.BatteryChemistry
import ua.kpi.battery_storage.lab6.domain.BatteryFormValidator
import ua.kpi.battery_storage.lab6.domain.PasswordStrength
import ua.kpi.battery_storage.ui.theme.ChargeHigh
import ua.kpi.battery_storage.ui.theme.ChargeLow
import ua.kpi.battery_storage.ui.theme.ChargeMedium

/** Заголовок форми: назва та короткий опис призначення. */
@Composable
fun FormHeader(title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Універсальне поле з валідацією: показує підказку, доки немає помилки,
 * і текст помилки червоним, якщо значення некоректне. Використовується для всіх текстових полів.
 */
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    error: String?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = { FieldHint(error, hint) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Підказка або повідомлення про помилку під полем. */
@Composable
fun FieldHint(error: String?, hint: String) {
    Text(
        text = error ?: hint,
        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Поле пароля з індикатором складності та переліком невиконаних критеріїв.
 * Складність обчислює BatteryFormValidator, компонент лише відображає результат.
 */
@Composable
fun PasswordFieldWithStrength(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Пароль") },
            isError = error != null,
            supportingText = { FieldHint(error, "Великі й малі літери, цифра, спецсимвол") },
            trailingIcon = {
                TextButton(onClick = { visible = !visible }) { Text(if (visible) "Сховати" else "Показати") }
            },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isNotEmpty()) {
            val strength = BatteryFormValidator.passwordStrength(value)
            val color = strengthColor(strength)
            LinearProgressIndicator(
                progress = { strength.score / 3f },
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
            Text(
                "Складність: ${strength.title}",
                color = color,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun strengthColor(strength: PasswordStrength): Color = when (strength) {
    PasswordStrength.WEAK -> ChargeLow
    PasswordStrength.MEDIUM -> ChargeMedium
    PasswordStrength.STRONG -> ChargeHigh
}

/** Вибір типу хімії акумуляторів; від нього залежить перевірка потужності. */
@Composable
fun ChemistrySelector(selected: BatteryChemistry, onSelect: (BatteryChemistry) -> Unit, enabled: Boolean) {
    Column {
        Text("Тип хімії", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BatteryChemistry.entries.forEach { chemistry ->
                FilterChip(
                    selected = chemistry == selected,
                    onClick = { onSelect(chemistry) },
                    label = { Text("${chemistry.title} · ${chemistry.maxCRate}C") },
                    enabled = enabled,
                )
            }
        }
    }
}

/** Кнопка відправлення: під час обробки замість тексту показує індикатор. */
@Composable
fun SubmitButton(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text)
        }
    }
}

/** Рядок стану завантаження з поясненням, яка саме операція виконується. */
@Composable
fun LoadingIndicator(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(message, modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Картка з результатом обробки: помилка (червона) або успіх (зелена). */
@Composable
fun ResultMessage(text: String, isError: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isError) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Box(modifier = Modifier.padding(start = 10.dp)) { Text(text) }
        }
    }
}
