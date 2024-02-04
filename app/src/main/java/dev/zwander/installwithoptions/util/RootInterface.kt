package dev.zwander.installwithoptions.util

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

class RootInterface : RootService() {
    override fun onBind(intent: Intent): IBinder {
        return ShellInterface(this)
    }
}
