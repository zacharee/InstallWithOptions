package dev.zwander.installwithoptions

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass

class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.setHiddenApiExemptions("")
        }

        context = this
    }
}
