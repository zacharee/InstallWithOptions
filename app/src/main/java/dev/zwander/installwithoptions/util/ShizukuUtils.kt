package dev.zwander.installwithoptions.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

object ShizukuUtils {
    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getApplicationInfo(ShizukuProvider.MANAGER_APPLICATION_ID, 0) != null
        } catch (e: Throwable) {
            false
        }
    }

    val isRunning: Boolean
        get() = Shizuku.pingBinder()

    @Composable
    fun rememberShizukuState(): State<ShizukuState> {
        val context = LocalContext.current
        val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
        val shizukuState = remember {
            mutableStateOf(ShizukuState.NOT_INSTALLED)
        }

        LaunchedEffect(key1 = lifecycleState) {
            if (lifecycleState == Lifecycle.State.RESUMED) {
                val installed = isInstalled(context)
                val isRunning = isRunning

                shizukuState.value = when {
                    !installed -> ShizukuState.NOT_INSTALLED
                    installed && !isRunning -> ShizukuState.INSTALLED_NOT_RUNNING
                    else -> ShizukuState.RUNNING
                }
            }
        }

        return shizukuState
    }
}

enum class ShizukuState {
    NOT_INSTALLED,
    INSTALLED_NOT_RUNNING,
    RUNNING,
}
