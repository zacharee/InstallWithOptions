package dev.zwander.installwithoptions.data

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.zwander.installwithoptions.App
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object Settings {
    object Keys {
        val selectedOptions = SettingsKey.Complex(
            key = "selectedOptions",
            default = listOf(),
            serializer = { options ->
                options?.map { it.value }
                    ?.let { gson.toJson(it) }
            },
            deserializer = {
                if (it == null) {
                    listOf()
                } else {
                    val values = gson.fromJson<ArrayList<Int>>(
                        it,
                        object : TypeToken<ArrayList<Int>>() {}.type,
                    )

                    values.mapNotNull { value -> getInstallOptions().find { opt -> opt.value == value } }
                }
            },
            settings = settings,
        )
    }

    val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
    val gson = GsonBuilder().create()
}

sealed class SettingsKey<Type> {
    abstract val key: kotlin.String
    abstract val default: Type?
    abstract val settings: SharedPreferences

    abstract fun getValue(): Type?
    abstract fun setValue(value: Type?)

    protected fun registerListener(callback: (Type?) -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == this@SettingsKey.key) {
                    callback(getValue())
                }
            }

        settings.registerOnSharedPreferenceChangeListener(listener)

        return listener
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun asMutableStateFlow(): MutableStateFlow<Type?> {
        val wrappedFlow = MutableStateFlow(getValue())
        val flow = object : MutableStateFlow<Type?> by wrappedFlow {
            override var value: Type?
                get() = wrappedFlow.value
                set(value) {
                    wrappedFlow.value = value
                    GlobalScope.launch(Dispatchers.IO) {
                        setValue(value)
                    }
                }

            override suspend fun emit(value: Type?) {
                wrappedFlow.emit(value)
                GlobalScope.launch(Dispatchers.IO) {
                    setValue(value)
                }
            }

            override fun tryEmit(value: Type?): kotlin.Boolean {
                return wrappedFlow.tryEmit(value).also {
                    if (it) {
                        GlobalScope.launch(Dispatchers.IO) {
                            setValue(value)
                        }
                    }
                }
            }
        }

        registerListener {
            flow.value = it
        }

        return flow
    }

    @Composable
    fun collectAsMutableState(): MutableState<Type?> {
        val state = remember {
            mutableStateOf(getValue())
        }

        LaunchedEffect(state.value) {
            setValue(state.value)
        }

        DisposableEffect(this) {
            val listener = registerListener {
                state.value = getValue()
            }

            onDispose {
                settings.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

        return state
    }

    operator fun invoke(): Type? = getValue()

    data class Boolean(
        override val key: kotlin.String,
        override val default: kotlin.Boolean?,
        override val settings: SharedPreferences,
    ) : SettingsKey<kotlin.Boolean>() {
        override fun getValue(): kotlin.Boolean? {
            return default?.let { settings.getBoolean(key, default) }
        }

        override fun setValue(value: kotlin.Boolean?) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putBoolean(key, value)
                }
            }
        }
    }

    data class Int(
        override val key: kotlin.String,
        override val default: kotlin.Int?,
        override val settings: SharedPreferences,
    ) : SettingsKey<kotlin.Int>() {
        override fun getValue(): kotlin.Int? {
            return default?.let { settings.getInt(key, default) }
        }

        override fun setValue(value: kotlin.Int?) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putInt(key, value)
                }
            }
        }
    }

    data class Long(
        override val key: kotlin.String,
        override val default: kotlin.Long?,
        override val settings: SharedPreferences,
    ) : SettingsKey<kotlin.Long>() {
        override fun getValue(): kotlin.Long? {
            return default?.let { settings.getLong(key, default) }
        }

        override fun setValue(value: kotlin.Long?) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putLong(key, value)
                }
            }
        }
    }

    data class String(
        override val key: kotlin.String,
        override val default: kotlin.String?,
        override val settings: SharedPreferences,
    ) : SettingsKey<kotlin.String>() {
        override fun getValue(): kotlin.String? {
            return default?.let { settings.getString(key, default) }
        }

        override fun setValue(value: kotlin.String?) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putString(key, value)
                }
            }
        }
    }

    data class Float(
        override val key: kotlin.String,
        override val default: kotlin.Float?,
        override val settings: SharedPreferences,
    ) : SettingsKey<kotlin.Float>() {
        override fun getValue(): kotlin.Float? {
            return default?.let { settings.getFloat(key, default) }
        }

        override fun setValue(value: kotlin.Float?) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putFloat(key, value)
                }
            }
        }
    }

    data class Double(
        override val key: kotlin.String,
        override val default: kotlin.Double,
        override val settings: SharedPreferences,
    ) : SettingsKey<kotlin.Double>() {
        override fun getValue(): kotlin.Double {
            return settings.getString(key, default.toString()).toDoubleOrNull() ?: default
        }

        override fun setValue(value: kotlin.Double?) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putString(key, value.toString())
                }
            }
        }
    }

    data class Complex<Type>(
        override val key: kotlin.String,
        override val default: Type?,
        val deserializer: (kotlin.String?) -> Type?,
        val serializer: (Type?) -> kotlin.String?,
        override val settings: SharedPreferences,
    ) : SettingsKey<Type>() {
        override fun getValue(): Type? {
            return deserializer(default?.let { settings.getString(key, serializer(default)) })
        }

        override fun setValue(value: Type?) {
            val serialized = serializer(value)

            settings.edit {
                if (serialized == null) {
                    remove(key)
                } else {
                    putString(key, serialized)
                }
            }
        }
    }
}