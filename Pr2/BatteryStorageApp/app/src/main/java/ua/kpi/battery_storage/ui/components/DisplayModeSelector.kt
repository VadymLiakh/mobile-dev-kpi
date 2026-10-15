package ua.kpi.battery_storage.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.kpi.battery_storage.DisplayFor
import ua.kpi.battery_storage.Lab

/**
 * Перемикач у верхній частині екрана: перший ряд обирає практичне заняття,
 * другий — домашнє завдання або рівень складності. Нижче показано тему заняття.
 */
@Composable
fun DisplayModeSelector(
    lab: Lab,
    displayFor: DisplayFor,
    onLabChange: (Lab) -> Unit,
    onDisplayForChange: (DisplayFor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SegmentedSelector(
            options = Lab.entries,
            selected = lab,
            label = { it.shortTitle },
            onSelect = onLabChange,
        )
        SegmentedSelector(
            options = DisplayFor.entries,
            selected = displayFor,
            label = { it.title },
            onSelect = onDisplayForChange,
        )
        Text(
            text = lab.topic,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Ряд сегментованих кнопок з одиничним вибором; галочку прибрано, щоб підписи вміщалися. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {},
                label = {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}
