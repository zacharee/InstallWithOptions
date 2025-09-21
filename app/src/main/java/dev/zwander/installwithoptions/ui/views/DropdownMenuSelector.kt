package dev.zwander.installwithoptions.ui.views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.zwander.installwithoptions.R
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

    CompositionLocalProvider(
        LocalTextToolbar provides NoOpTextToolbar,
    ) {
        Box {
            OutlinedTextField(
                value = value?.label() ?: "",
                onValueChange = {},
                trailingIcon = {
                    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation),
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
                modifier = modifier
                    .clickable {
                        expanded = true
                    },
                readOnly = true,
            )

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
                            expanded = false
                            onValueChanged(opt)
                        },
                        trailingIcon = {
                            if (opt.value == value?.value) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.selected_option),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}