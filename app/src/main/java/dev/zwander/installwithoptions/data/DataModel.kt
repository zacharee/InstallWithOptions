package dev.zwander.installwithoptions.data

import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow

object DataModel {
    val shizukuAvailable = MutableStateFlow(false)
    val rootGranted = MutableStateFlow(false)
    val selectedOptions = Settings.Keys.selectedOptions.asMutableStateFlow()
    val selectedFiles = MutableStateFlow(mapOf<String, List<DocumentFile>>())
}
