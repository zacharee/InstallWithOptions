package dev.zwander.installwithoptions.util

import android.util.Log

fun Throwable.extractErrorMessage(): String? {
    val mainMessage = localizedMessage ?: message

    Log.e("InstallWithOptions", "Got message ${mainMessage}", this)

    if (mainMessage != null && !mainMessage.endsWith("null")) {
        return mainMessage
    }

    return cause?.extractErrorMessage()
}
