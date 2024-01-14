package dev.zwander.installwithoptions.data

import android.os.Build

sealed class BaseOption<T> {
    open val minSdk: Int = Build.VERSION_CODES.BASE
    open val maxSdk: Int = Int.MAX_VALUE
    abstract val value: T
    abstract val labelResource: Int
    abstract val descResource: Int
}
