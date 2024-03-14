package dev.zwander.installwithoptions.data

import android.content.pm.PackageInstaller.SessionParams
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.zwander.installwithoptions.R
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

sealed class MutableOption<T>(
    val settingsKey: SettingsKey<T>,
    val operator: SessionParams.(value: T?) -> Unit,
    override val minSdk: Int = Build.VERSION_CODES.BASE,
    override val maxSdk: Int = Int.MAX_VALUE,
    @StringRes override val labelResource: Int,
    @StringRes override val descResource: Int,
) : BaseOption<MutableStateFlow<T?>>() {
    override val value = settingsKey.asMutableStateFlow()

    fun apply(params: SessionParams) {
        params.operator(value.value)
    }

    @Keep
    data object InstallerPackage : MutableOption<String>(
        settingsKey = SettingsKey.String(
            key = "installer_package",
            default = null,
            settings = Settings.settings,
        ),
        operator = {
            if (!it.isNullOrBlank()) {
                setInstallerPackageName(it)
            }
        },
        labelResource = R.string.installer_package,
        descResource = R.string.installer_package_desc,
    )
}
