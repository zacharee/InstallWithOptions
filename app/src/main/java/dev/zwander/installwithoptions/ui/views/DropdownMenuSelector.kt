package dev.zwander.installwithoptions.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalTextToolbar
import dev.zwander.installwithoptions.util.NoOpTextToolbar

data class Option<T>(
    val label: @Composable () -> String,
    val value: T,
)

@Composable
fun <T> DropdownMenuSelector(
    modifier: Modifier = Modifier,
    value: Option<T>?,
    onValueChanged: (Option<T>) -> Unit,
    values: Collection<Option<T>>,
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(modifier = modifier) {
        CompositionLocalProvider(
            LocalTextToolbar provides NoOpTextToolbar,
        ) {
            OutlinedTextField(
                value = value?.label() ?: "",
                onValueChange = {},
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                    )
                },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors().run {
                    copy(
                        disabledContainerColor = unfocusedContainerColor,
                        disabledPrefixColor = unfocusedPrefixColor,
                        disabledSuffixColor = unfocusedSuffixColor,
                        disabledTextColor = unfocusedTextColor,
                        disabledIndicatorColor = unfocusedIndicatorColor,
                        disabledLabelColor = unfocusedLabelColor,
                        disabledPlaceholderColor = unfocusedPlaceholderColor,
                        disabledLeadingIconColor = unfocusedLeadingIconColor,
                        disabledTrailingIconColor = unfocusedTrailingIconColor,
                        disabledSupportingTextColor = unfocusedSupportingTextColor,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = true
                    },
                readOnly = true,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            values.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(text = opt.label())
                    },
                    onClick = {
                        onValueChanged(opt)
                        expanded = false
                    },
                )
            }
        }
    }
}