package dev.zwander.installwithoptions.util

import android.content.Context
import dev.zwander.installwithoptions.R

fun Context.getSpecificErrorMessage(legacyStatus: Int): String? {
    return when (legacyStatus) {
        // INSTALL_FAILED_NO_MATCHING_ABIS
        -113 -> resources.getString(R.string.install_failed_no_matching_abis)
        else -> null
    }
}
