package dev.zwander.installwithoptions.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.topjohnwu.superuser.ipc.RootService
import dev.zwander.installwithoptions.BuildConfig
import dev.zwander.installwithoptions.IShellInterface
import dev.zwander.installwithoptions.R
import dev.zwander.installwithoptions.data.DataModel
import rikka.shizuku.Shizuku

class ShizukuRootAdapter(private val context: Context) {
    private val shizukuArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context, ShellInterface::class.java),
        )
            .tag("shell_service")
            .processNameSuffix("shell_service")
            .debuggable(BuildConfig.DEBUG)
            .daemon(false)
    }
    private val rootServiceIntent by lazy {
        Intent(context, RootInterface::class.java)
    }

    @Composable
    fun rememberShellInterface(): IShellInterface? {
        val shizukuAvailable by DataModel.shizukuAvailable.collectAsState()
        val rootGranted by DataModel.rootGranted.collectAsState()

        var shellInterface by remember {
            mutableStateOf<IShellInterface?>(null)
        }

        DisposableEffect(key1 = shizukuAvailable, key2 = rootGranted, key3 = context) {
            val adapter = ShizukuRootAdapter(context)
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    shellInterface = IShellInterface.Stub.asInterface(service)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    shellInterface = null
                }
            }

            val shizukuAvailableFrozen = shizukuAvailable
            val rootGrantedFrozen = rootGranted

            val mode = when {
                rootGrantedFrozen -> Mode.ROOT
                shizukuAvailableFrozen -> Mode.SHIZUKU
                else -> Mode.NONE
            }

            adapter.bindService(
                connection = connection,
                mode = mode,
            )

            onDispose {
                adapter.unbindService(
                    connection = connection,
                    mode = mode,
                )
                shellInterface = null
            }
        }

        return shellInterface
    }

    private fun bindService(connection: ServiceConnection, mode: Mode) {
        if (mode == Mode.NONE) {
            return
        }

        try {
            when (mode) {
                Mode.SHIZUKU -> Shizuku.bindUserService(shizukuArgs, connection)
                Mode.ROOT -> RootService.bind(rootServiceIntent, connection)
                else -> {}
            }
        } catch (e: Throwable) {
            Toast.makeText(context, R.string.error_binding_service, Toast.LENGTH_SHORT).show()
        }
    }

    private fun unbindService(connection: ServiceConnection, mode: Mode) {
        if (mode == Mode.NONE) {
            return
        }

        try {
            when (mode) {
                Mode.SHIZUKU -> Shizuku.unbindUserService(shizukuArgs, connection, true)
                Mode.ROOT -> RootService.unbind(connection)
                else -> {}
            }
        } catch (_: Exception) {}
    }

    enum class Mode {
        SHIZUKU,
        ROOT,
        NONE,
    }
}
