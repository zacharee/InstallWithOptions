package dev.zwander.installwithoptions.data

import kotlinx.coroutines.flow.MutableStateFlow

object DataModel {
    val shizukuAvailable = MutableStateFlow(false)
    val rootGranted = MutableStateFlow(false)
    val selectedOptions = Settings.Keys.selectedOptions.asMutableStateFlow()
}
