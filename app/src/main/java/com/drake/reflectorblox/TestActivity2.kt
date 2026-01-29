package com.drake.reflectorblox

import android.os.Bundle
import com.drake.reflectorblox.dexactivity.DexBasedActivity
import java.io.File

class TestActivity2 : DexBasedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val filesDir = this.filesDir
        val apkFile = File(filesDir, "app-release.apk")
        apkFile.setReadOnly()
        apkPath = apkFile.absolutePath
        libPath = applicationContext.applicationInfo.nativeLibraryDir // TODO
        activityClassName = "com.drake.testappforrobloxdex.MainActivity"
        applicationClassName = "com.drake.testappforrobloxdex.Application"

        super.onCreate(savedInstanceState)
    }
}