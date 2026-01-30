package com.drake.reflectorblox

import android.os.Bundle
import com.drake.reflectorblox.dexactivity.DexBasedActivity
import java.io.File

class TestActivity : DexBasedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val filesDir = this.filesDir
        val apkFile = File(filesDir, "base.apk")
        apkFile.setReadOnly()
        apkPath = apkFile.absolutePath
        libPath = File(filesDir, "robloxlibs").absolutePath //applicationContext.applicationInfo.nativeLibraryDir // TODO
        activityClassName = "com.roblox.client.ActivitySplash"
        applicationClassName = "com.roblox.client.RobloxApplication"
        hookActivity("com.roblox.client.ActivityNativeMain",
            TestActivity2::class.java as Class<DexBasedActivity>
        ) //wtf im doing
        super.onCreate(savedInstanceState)
    }
}