@file:Suppress("DEPRECATION")

package dev.zwander.installwithoptions.util

import android.content.Context
import android.content.pm.PackageParser
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.bugsnag.android.Bugsnag
import dev.zwander.installwithoptions.data.DataModel
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File

fun Context.handleIncomingUris(uris: List<Uri>) {
    DataModel.isImporting.value = true
    val currentSelection = DataModel.selectedFiles.value.toMutableMap()

    uris.forEach { uri ->
        val file = DocumentFile.fromSingleUri(this, uri) ?: return@forEach

        if (file.isApk) {
            addApkFile(file, currentSelection)
        } else if (file.isSplitBundle) {
            copyZipToCacheAndExtract(file).forEach { innerFile ->
                addApkFile(innerFile, currentSelection)
            }
        }
    }

    DataModel.selectedFiles.value = currentSelection
    DataModel.isImporting.value = false
}

private fun Context.addApkFile(file: DocumentFile, currentSelection: MutableMap<String, List<DocumentFile>>) {
    try {
        val realFile = if (file.uri.scheme == "file") file else run {
            contentResolver.openInputStream(file.uri).use { input ->
                val dest = File(cacheDir, "${file.name ?: file.uri}")
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
                DocumentFile.fromFile(dest)
            }
        }
        val apkFile = PackageParser.parseApkLite(File(realFile.uri.path), 0)
        val packageList = currentSelection[apkFile.packageName] ?: listOf()

        currentSelection[apkFile.packageName] = (packageList + file).distinctBy { "${apkFile.packageName}:${it.name}" }
    } catch (_: Throwable) {}
}

private fun Context.copyZipToCacheAndExtract(zip: DocumentFile): List<DocumentFile> {
    val destFile = File(cacheDir, zip.name ?: zip.uri.toString())
    val destDir = File(cacheDir, "${destFile.name}_extracted").apply {
        deleteRecursively()
        mkdirs()
    }

    contentResolver.openInputStream(zip.uri).use { input ->
        destFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    val zipFile = ZipFile(destFile)

    try {
        zipFile.extractAll(destDir.absolutePath)
    } catch (e: ZipException) {
        Bugsnag.notify(e)
        return listOf()
    }

    return destDir.listFiles()?.mapNotNull { file ->
        val documentFile = DocumentFile.fromFile(file)

        if (documentFile.isApk) documentFile else null
    } ?: listOf()
}

private val DocumentFile.isApk: Boolean
    get() = type == "application/vnd.android.package-archive" || name?.endsWith(".apk") == true

private val DocumentFile.isSplitBundle: Boolean
    get() = type == "application/zip" || name?.endsWith(".xapk") == true || name?.endsWith(".apkm") == true || name?.endsWith(".apks") == true
