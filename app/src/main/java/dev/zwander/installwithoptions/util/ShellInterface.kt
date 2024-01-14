package dev.zwander.installwithoptions.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Looper
import dev.zwander.installwithoptions.IContentResolver
import dev.zwander.installwithoptions.IShellInterface
import kotlin.system.exitProcess

class ShellInterface(private val context: Context) : IShellInterface.Stub() {
    private val installer = InternalInstaller(context)

    @Suppress("UNCHECKED_CAST")
    override fun install(
        fileDescriptors: List<*>,
        fileUris: List<*>,
        options: List<*>,
        splits: Boolean,
    ) {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        val castedFileDescriptors = fileDescriptors as List<AssetFileDescriptor>
        val castedFiles = fileUris as List<Uri>
        val castedOptions = options as List<Int>

        installer.installPackage(castedFileDescriptors, castedFiles, castedOptions, splits)
    }

    override fun destroy() {
        exitProcess(0)
    }
}