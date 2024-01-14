package dev.zwander.installwithoptions.data

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import dev.zwander.installwithoptions.R

sealed class SessionOption(
    override val minSdk: Int = Build.VERSION_CODES.BASE,
    override val maxSdk: Int = Int.MAX_VALUE,
    override val value: Int,
    @StringRes override val labelResource: Int,
    @StringRes override val descResource: Int,
) : BaseOption<Int>() {
    data object RequestUpdateOwnership : SessionOption(
        value = PackageManager.INSTALL_REQUEST_UPDATE_OWNERSHIP,
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        labelResource = R.string.request_update_ownership,
        descResource = R.string.request_update_ownership_desc,
    )

    data object FromManagedUserOrProfile : SessionOption(
        value = PackageManager.INSTALL_FROM_MANAGED_USER_OR_PROFILE,
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        labelResource = R.string.from_managed_user_or_profile,
        descResource = R.string.from_managed_user_or_profile_desc,
    )

    data object IgnoreDexoptProfile : SessionOption(
        value = 1 shl 28,
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        labelResource = R.string.ignore_dexopt_profile,
        descResource = R.string.ignore_dexopt_profile_desc,
    )
}
