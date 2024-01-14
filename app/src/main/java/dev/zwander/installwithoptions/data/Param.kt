package dev.zwander.installwithoptions.data

import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.StringRes

sealed class Param(
    override val minSdk: Int = Build.VERSION_CODES.BASE,
    override val maxSdk: Int = Int.MAX_VALUE,
    override val value: String,
    @StringRes override val labelResource: Int,
    @StringRes override val descResource: Int,
) : BaseOption<String>() {
    abstract fun apply(params: PackageInstaller.SessionParams)
}
