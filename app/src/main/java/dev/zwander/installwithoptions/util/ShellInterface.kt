package dev.zwander.installwithoptions.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Looper
import dev.zwander.installwithoptions.IShellInterface
import kotlin.system.exitProcess

class ShellInterface(context: Context) : IShellInterface.Stub() {
    private val realContext = context.createPackageContext("com.android.shell", 0)
    private val installer = InternalInstaller(realContext)

    @Suppress("UNCHECKED_CAST")
    override fun install(
        fileDescriptors: List<*>,
        options: List<*>,
        splits: Boolean,
    ) {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        val castedFileDescriptors = fileDescriptors as List<AssetFileDescriptor>
        val castedOptions = options as List<Int>

        installer.installPackage(castedFileDescriptors, castedOptions, splits)
    }

    override fun destroy() {
        exitProcess(0)
    }
}