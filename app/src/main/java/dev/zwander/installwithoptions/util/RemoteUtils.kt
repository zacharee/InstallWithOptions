package dev.zwander.installwithoptions.util

import android.content.ContentResolver
import android.net.Uri
import dev.zwander.installwithoptions.IContentResolver
import dev.zwander.installwithoptions.IRemoteInputStream
import dev.zwander.installwithoptions.IRemoteOutputStream
import java.io.InputStream
import java.io.OutputStream

fun ContentResolver.toRemoteContentResolver(): IContentResolver {
    return object : IContentResolver.Stub() {
        override fun openInputStream(uri: Uri?): IRemoteInputStream {
            return this@toRemoteContentResolver.openInputStream(uri).toRemoteInputStream()
        }

        override fun openOutputStream(uri: Uri?): IRemoteOutputStream {
            return this@toRemoteContentResolver.openOutputStream(uri).toRemoteOutputStream()
        }
    }
}

fun InputStream.toRemoteInputStream(): IRemoteInputStream {
    val buffered = this.buffered()

    return object : IRemoteInputStream.Stub() {
        override fun read(): Int {
            return buffered.read()
        }

        override fun close() {
            return buffered.close()
        }
    }
}

fun OutputStream.toRemoteOutputStream(): IRemoteOutputStream {
    val buffered = this.buffered()

    return object : IRemoteOutputStream.Stub() {
        override fun write(b: Int) {
            buffered.write(b)
        }

        override fun flush() {
            buffered.flush()
        }

        override fun close() {
            buffered.close()
        }
    }
}

fun IRemoteInputStream.toInputStream(): InputStream {
    return object : InputStream() {
        override fun read(): Int {
            return this@toInputStream.read()
        }

        override fun close() {
            this@toInputStream.close()
        }
    }
}

fun IRemoteOutputStream.toOutputStream(): OutputStream {
    return object : OutputStream() {
        override fun write(b: Int) {
            this@toOutputStream.write(b)
        }

        override fun flush() {
            this@toOutputStream.flush()
        }

        override fun close() {
            this@toOutputStream.close()
        }
    }
}
