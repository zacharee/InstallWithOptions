package dev.zwander.installwithoptions.util

import android.content.Context
import dev.zwander.installwithoptions.R

fun Context.getSpecificErrorMessage(legacyStatus: Int): String? {
    val res = when (legacyStatus) {
        // INSTALL_FAILED_ALREADY_EXISTS
        -1 -> R.string.install_failed_already_exists
        // INSTALL_FAILED_NO_MATCHING_ABIS
        -113 -> R.string.install_failed_no_matching_abis
        else -> null
    }

    return res?.let { resources.getString(it) }
}
