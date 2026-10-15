package ua.kpi.battery_storage.lab6

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.kpi.battery_storage.lab6.advanced.ChemistrySelector
import ua.kpi.battery_storage.lab6.advanced.FormHeader
import ua.kpi.battery_storage.lab6.advanced.LoadingIndicator
import ua.kpi.battery_storage.lab6.advanced.PasswordFieldWithStrength
import ua.kpi.battery_storage.lab6.advanced.ResultMessage
import ua.kpi.battery_storage.lab6.advanced.SubmitButton
import ua.kpi.battery_storage.lab6.advanced.ValidatedTextField
import ua.kpi.battery_storage.lab6.domain.BatteryChemistry
import ua.kpi.battery_storage.lab6.domain.BatteryFormValidator
import ua.kpi.battery_storage.lab6.domain.FormRules
import ua.kpi.battery_storage.lab6.domain.OperatorRegistry

/** Результат обробки форми для картки ResultMessage. */
private data class SubmitResult(val text: String, val isError: Boolean)

/**
 * Просунутий рівень ПЗ 6: форма поділена на окремі composable-компоненти,
 * перевірки виконує бізнес-логіка (BatteryFormValidator, OperatorRegistry):
 * складність пароля, C-rate потужності залежно від хімії, асинхронна перевірка
 * унікальності email. Під час обробки показується стан завантаження.
 * На широкому екрані поля розміщуються у дві колонки.
 */
@Composable
fun AdvancedFormApp() {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var capacity by rememberSaveable { mutableStateOf("") }
    var power by rememberSaveable { mutableStateOf("") }
    var chemistry by rememberSaveable { mutableStateOf(BatteryChemistry.LFP) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    // Стан асинхронної обробки
    val registry = remember { OperatorRegistry() }
    val scope = rememberCoroutineScope()
    var loadingMessage by remember { mutableStateOf<String?>(null) }
    var emailTakenError by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<SubmitResult?>(null) }
    val loading = loadingMessage != null

    fun check(value: String, rule: () -> String?) = if (value.isEmpty() && !submitted) null else rule()

    val nameError = check(name) { BatteryFormValidator.validateStationName(name) }
    val emailError = check(email) { FormRules.email(email) } ?: emailTakenError
    val passwordError = check(password) { BatteryFormValidator.validatePassword(password) }
    val capacityError = check(capacity) { BatteryFormValidator.validateCapacity(capacity) }
    val powerError = check(power) { BatteryFormValidator.validatePower(power, capacity, chemistry) }

    fun submit() {
        submitted = true
        result = null
        // Синхронні перевірки виконуються заново, бо submitted щойно змінився
        val errors = listOf(
            BatteryFormValidator.validateStationName(name),
            FormRules.email(email),
            BatteryFormValidator.validatePassword(password),
            BatteryFormValidator.validateCapacity(capacity),
            BatteryFormValidator.validatePower(power, capacity, chemistry),
        )
        if (errors.any { it != null }) {
            result = SubmitResult("Виправте помилки у формі (${errors.count { it != null }})", isError = true)
            return
        }
        scope.launch {
            try {
                loadingMessage = "Перевірка унікальності email…"
                if (registry.isEmailTaken(email)) {
                    emailTakenError = "Оператор з таким email уже зареєстрований"
                    result = SubmitResult("Email $email уже використовується", isError = true)
                    return@launch
                }
                loadingMessage = "Збереження даних системи…"
                registry.register(email)
                result = SubmitResult(
                    "Систему «${name.trim()}» (${capacity} кВт·год, ${power} кВт, ${chemistry.title}) зареєстровано",
                    isError = false,
                )
            } finally {
                loadingMessage = null
            }
        }
    }

    // Групи полів описано окремо, щоб скомпонувати їх у одну або дві колонки
    val systemFields: @Composable () -> Unit = {
        ValidatedTextField(name, { name = it; result = null }, "Назва системи", "3–40 символів", nameError, enabled = !loading)
        ValidatedTextField(
            capacity, { capacity = it; result = null }, "Ємність, кВт·год",
            "${BatteryFormValidator.MIN_CAPACITY_KWH.toInt()}–${BatteryFormValidator.MAX_CAPACITY_KWH.toInt()}",
            capacityError, keyboardType = KeyboardType.Decimal, enabled = !loading,
        )
        ChemistrySelector(chemistry, { chemistry = it }, enabled = !loading)
        ValidatedTextField(
            power, { power = it; result = null }, "Потужність інвертора, кВт",
            "Не більше ємність × ${chemistry.maxCRate}C", powerError,
            keyboardType = KeyboardType.Decimal, enabled = !loading,
        )
    }
    val operatorFields: @Composable () -> Unit = {
        ValidatedTextField(
            email, { email = it; emailTakenError = null; result = null }, "Email оператора",
            "Має бути унікальним (зайняті: admin@bess.ua, operator@bess.ua)", emailError,
            keyboardType = KeyboardType.Email, enabled = !loading,
        )
        PasswordFieldWithStrength(password, { password = it; result = null }, passwordError, enabled = !loading)
        loadingMessage?.let { LoadingIndicator(it) }
        result?.let { ResultMessage(it.text, it.isError) }
        SubmitButton("Зареєструвати систему", loading = loading, onClick = ::submit)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormHeader("Реєстрація BESS", "Комплексна перевірка даних накопичувача та оператора")
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { systemFields() }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { operatorFields() }
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    systemFields()
                    operatorFields()
                }
            }
        }
    }
}
