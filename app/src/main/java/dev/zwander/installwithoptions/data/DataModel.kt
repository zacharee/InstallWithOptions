package dev.zwander.installwithoptions.data

import kotlinx.coroutines.flow.MutableStateFlow

object DataModel {
    val shizukuGranted = MutableStateFlow(false)
    val selectedOptions = Settings.Keys.selectedOptions.asMutableStateFlow()
}
