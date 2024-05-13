package dev.zwander.installwithoptions.data

import android.annotation.SuppressLint
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.icerock.moko.mvvm.flow.compose.collectAsMutableState
import dev.zwander.installwithoptions.R
import dev.zwander.installwithoptions.util.NoOpTextToolbar
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun rememberMutableOptions(): List<MutableOption<*>> {
    val context = LocalContext.current

    return remember {
        getMutableOptions().sortedBy { opt ->
            context.resources.getString(opt.labelResource)
        }
    }
}

fun getMutableOptions() = MutableOption::class.sealedSubclasses
    .mapNotNull { it.objectInstance }
    .filter { Build.VERSION.SDK_INT >= it.minSdk && Build.VERSION.SDK_INT <= it.maxSdk }

sealed class MutableOption<T : Any?>(
    val settingsKey: SettingsKey<T>,
    val operator: SessionParams.(value: T) -> Unit,
    override val minSdk: Int = Build.VERSION_CODES.BASE,
    override val maxSdk: Int = Int.MAX_VALUE,
    @StringRes override val labelResource: Int,
    @StringRes override val descResource: Int,
) : BaseOption<MutableStateFlow<T>>() {
    override val value = settingsKey.asMutableStateFlow()

    fun apply(params: SessionParams) {
        params.operator(value.value)
    }

    @Composable
    fun Render(modifier: Modifier) {
        OutlinedCard(
            modifier = modifier,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(id = labelResource),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Text(
                        text = stringResource(id = descResource),
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Spacer(modifier = Modifier.size(4.dp))

                    RenderValueSelector(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    @Composable
    abstract fun RenderValueSelector(modifier: Modifier)

    @Keep
    data object InstallerPackage : MutableOption<String?>(
        settingsKey = SettingsKey.String(
            key = "installer_package",
            default = null,
            settings = Settings.settings,
        ),
        operator = {
            @SuppressLint("NewApi")
            if (!it.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setInstallerPackageName(it)
            }
        },
        labelResource = R.string.installer_package,
        descResource = R.string.installer_package_desc,
    ) {
        @Composable
        override fun RenderValueSelector(modifier: Modifier) {
            var state by value.collectAsMutableState()

            OutlinedTextField(
                value = state ?: "",
                onValueChange = { state = it },
                modifier = modifier,
            )
        }
    }

    @SuppressLint("InlinedApi")
    @Keep
    data object InstallReason : MutableOption<Int>(
        settingsKey = SettingsKey.Int(
            key = "install_reason",
            default = PackageManager.INSTALL_REASON_UNKNOWN,
            settings = Settings.settings,
        ),
        operator = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setInstallReason(it)
            }
        },
        labelResource = R.string.install_reason,
        descResource = R.string.install_reason_desc,
        minSdk = Build.VERSION_CODES.O,
    ) {
        data class Option(
            @StringRes
            val labelRes: Int,
            val value: Int,
        )

        private val options = mutableMapOf(
            PackageManager.INSTALL_REASON_UNKNOWN to Option(
                labelRes = R.string.install_reason_unknown,
                value = PackageManager.INSTALL_REASON_UNKNOWN,
            ),
            PackageManager.INSTALL_REASON_POLICY to Option(
                labelRes = R.string.install_reason_policy,
                value = PackageManager.INSTALL_REASON_POLICY,
            ),
            PackageManager.INSTALL_REASON_DEVICE_RESTORE to Option(
                labelRes = R.string.install_reason_device_restore,
                value = PackageManager.INSTALL_REASON_DEVICE_RESTORE,
            ),
            PackageManager.INSTALL_REASON_DEVICE_SETUP to Option(
                labelRes = R.string.install_reason_device_setup,
                value = PackageManager.INSTALL_REASON_DEVICE_SETUP,
            ),
            PackageManager.INSTALL_REASON_USER to Option(
                labelRes = R.string.install_reason_user,
                value = PackageManager.INSTALL_REASON_USER,
            ),
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(
                    PackageManager.INSTALL_REASON_ROLLBACK,
                    Option(
                        labelRes = R.string.install_reason_rollback,
                        value = PackageManager.INSTALL_REASON_ROLLBACK,
                    ),
                )
            }
        }

        @Composable
        override fun RenderValueSelector(modifier: Modifier) {
            var state by value.collectAsMutableState()
            var expanded by remember {
                mutableStateOf(false)
            }

            Box(modifier = modifier) {
                CompositionLocalProvider(
                    LocalTextToolbar provides NoOpTextToolbar,
                ) {
                    OutlinedTextField(
                        value = options[state]?.let { stringResource(id = it.labelRes) } ?: "",
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
                    options.forEach { (_, opt) ->
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(id = opt.labelRes))
                            },
                            onClick = {
                                state = opt.value
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
