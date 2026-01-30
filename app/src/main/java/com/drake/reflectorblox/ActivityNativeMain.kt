package com.drake.reflectorblox

import android.os.Bundle
import com.drake.reflectorblox.dexactivity.DexBasedActivity
import java.io.File

class TestActivity2 : DexBasedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val filesDir = this.filesDir
        val apkFile = File(filesDir, "base.apk")
        apkFile.setReadOnly()
        apkPath = apkFile.absolutePath
        libPath = File(filesDir, "robloxlibs").absolutePath //applicationContext.applicationInfo.nativeLibraryDir // TODO
        activityClassName = "com.roblox.client.ActivityNativeMain"
        applicationClassName = "com.roblox.client.RobloxApplication"

        super.onCreate(savedInstanceState)
    }
}