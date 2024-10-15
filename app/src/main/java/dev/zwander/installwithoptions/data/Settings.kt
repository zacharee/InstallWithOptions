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
        val selectedOptions by lazy {
            SettingsKey.Complex(
                key = "selectedOptions",
                default = listOf(),
                serializer = { options ->
                    options.map { it.value }.let { gson.toJson(it) }
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

        val enableCrashReports by lazy {
            SettingsKey.Boolean(
                key = "enableCrashReports",
                default = false,
                settings = settings,
            )
        }
    }

    val settings by lazy {
        PreferenceManager.getDefaultSharedPreferences(App.context)
    }
    val gson = GsonBuilder().create()
}

@Suppress("unused", "UNCHECKED_CAST")
sealed class SettingsKey<Type : Any?> {
    abstract val key: kotlin.String
    abstract val default: Type
    abstract val settings: SharedPreferences

    abstract fun getValue(): Type
    abstract fun setValue(value: Type)

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
    fun asMutableStateFlow(): MutableStateFlow<Type> {
        val wrappedFlow = MutableStateFlow(getValue())
        val flow = object : MutableStateFlow<Type> by wrappedFlow {
            override var value: Type
                get() = wrappedFlow.value
                set(value) {
                    wrappedFlow.value = value
                    GlobalScope.launch(Dispatchers.IO) {
                        setValue(value)
                    }
                }

            override suspend fun emit(value: Type) {
                wrappedFlow.emit(value)
                GlobalScope.launch(Dispatchers.IO) {
                    setValue(value)
                }
            }

            override fun tryEmit(value: Type): kotlin.Boolean {
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
            flow.value = it ?: default
        }

        return flow
    }

    @Composable
    fun collectAsMutableState(): MutableState<Type> {
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

    operator fun invoke(): Type = getValue()
    operator fun invoke(value: Type) = setValue(value)

    fun getAndSetDefaultIfNonExistent(defValue: Type): Type {
        return if (settings.contains(key)) {
            getValue()
        } else {
            setValue(defValue)
            defValue
        }
    }

    data class Boolean<T : kotlin.Boolean?>(
        override val key: kotlin.String,
        override val default: T,
        override val settings: SharedPreferences,
    ) : SettingsKey<T>() {
        override fun getValue(): T {
            return default?.let { settings.getBoolean(key, default) } as T
        }

        override fun setValue(value: T) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putBoolean(key, value)
                }
            }
        }
    }

    data class Int<T : kotlin.Int?>(
        override val key: kotlin.String,
        override val default: T,
        override val settings: SharedPreferences,
    ) : SettingsKey<T>() {
        override fun getValue(): T {
            return default?.let { settings.getInt(key, default) } as T
        }

        override fun setValue(value: T) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putInt(key, value)
                }
            }
        }
    }

    data class Long<T : kotlin.Long?>(
        override val key: kotlin.String,
        override val default: T,
        override val settings: SharedPreferences,
    ) : SettingsKey<T>() {
        override fun getValue(): T {
            return default?.let { settings.getLong(key, default) } as T
        }

        override fun setValue(value: T) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putLong(key, value)
                }
            }
        }
    }

    data class String<T : kotlin.String?>(
        override val key: kotlin.String,
        override val default: T,
        override val settings: SharedPreferences,
    ) : SettingsKey<T>() {
        override fun getValue(): T {
            return settings.getString(key, default) as T
        }

        override fun setValue(value: T) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putString(key, value)
                }
            }
        }
    }

    data class Float<T : kotlin.Float?>(
        override val key: kotlin.String,
        override val default: T,
        override val settings: SharedPreferences,
    ) : SettingsKey<T>() {
        override fun getValue(): T {
            return default?.let { settings.getFloat(key, default) } as T
        }

        override fun setValue(value: T) {
            settings.edit {
                if (value == null) {
                    remove(key)
                } else {
                    putFloat(key, value)
                }
            }
        }
    }

    data class Double<T : kotlin.Double?>(
        override val key: kotlin.String,
        override val default: T,
        override val settings: SharedPreferences,
    ) : SettingsKey<T>() {
        override fun getValue(): T {
            return (settings.getString(key, default.toString())?.toDoubleOrNull() ?: default) as T
        }

        override fun setValue(value: T) {
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
        override val default: Type,
        val deserializer: (kotlin.String?) -> Type,
        val serializer: (Type) -> kotlin.String?,
        override val settings: SharedPreferences,
    ) : SettingsKey<Type>() {
        override fun getValue(): Type {
            return deserializer(default?.let { settings.getString(key, serializer(default)) })
        }

        override fun setValue(value: Type) {
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

sealed interface IOptionItem {
    val label: String
    val desc: String?
    val listKey: String

    data class ActionOptionItem(
        override val label: String,
        override val desc: String?,
        override val listKey: String,
        val action: suspend () -> Unit,
    ) : IOptionItem

    sealed interface BasicOptionItem<T> : IOptionItem {
        val key: SettingsKey<T>
        override val listKey: String
            get() = key.key

        data class BooleanItem(
            override val label: String,
            override val desc: String?,
            override val key: SettingsKey<Boolean>,
        ) : BasicOptionItem<Boolean>
    }
}
