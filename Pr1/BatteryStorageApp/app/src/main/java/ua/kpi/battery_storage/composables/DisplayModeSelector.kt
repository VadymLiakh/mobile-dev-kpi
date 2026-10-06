package ua.kpi.battery_storage.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.kpi.battery_storage.DisplayFor

/**
 * Перемикач режимів відображення: кнопки по дві в рядку,
 * обраний режим виділено заповненою кнопкою.
 *
 * @param selected поточний режим
 * @param onSelectedChange викликається при виборі іншого режиму
 */
@Composable
fun DisplayModeSelector(
    selected: DisplayFor,
    onSelectedChange: (DisplayFor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Вибір режиму відображення",
            style = MaterialTheme.typography.titleSmall,
        )
        DisplayFor.entries.chunked(2).forEach { rowModes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowModes.forEach { mode ->
                    val buttonModifier = Modifier.weight(1f)
                    val label: @Composable () -> Unit = {
                        Text(text = mode.title, textAlign = TextAlign.Center)
                    }
                    if (mode == selected) {
                        Button(onClick = {}, modifier = buttonModifier) { label() }
                    } else {
                        OutlinedButton(
                            onClick = { onSelectedChange(mode) },
                            modifier = buttonModifier,
                        ) { label() }
                    }
                }
            }
        }
    }
}
