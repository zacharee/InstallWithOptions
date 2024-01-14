package dev.zwander.installwithoptions.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Looper
import dev.zwander.installwithoptions.IShellInterface
import kotlin.system.exitProcess

class ShellInterface(context: Context) : IShellInterface.Stub() {
    private val realContext = context.createPackageContext("com.android.shell", 0)
    private val installer = InternalInstaller(realContext)

    override fun install(
        fileDescriptors: Array<AssetFileDescriptor>,
        options: IntArray,
        splits: Boolean,
    ) {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        installer.installPackage(fileDescriptors, options, splits)
    }

    override fun destroy() {
        exitProcess(0)
    }
}