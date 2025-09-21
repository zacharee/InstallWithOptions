package dev.zwander.installwithoptions.data

import androidx.annotation.StringRes
import dev.zwander.installwithoptions.R

enum class Theme(@StringRes val labelRes: Int, val key: String) {
    LIGHT(R.string.theme_light, "theme_light"),
    DARK(R.string.theme_dark, "theme_dark"),
    SYSTEM(R.string.theme_system, "theme_system");

    companion object {
        fun fromKey(key: String, def: Theme): Theme {
            return entries.firstOrNull { it.key == key } ?: def
        }
    }
}
