package com.drake.reflectorblox

import com.drake.reflectorblox.robloxactivity.RobloxActivity

class TestAppActivity : RobloxActivity() {
    override val applicationClassName = "com.drake.testappforrobloxdex.Application"
    override val activityClassName = "com.drake.testappforrobloxdex.MainActivity2"
    override val activityRedirections = mapOf(
        "com.drake.testappforrobloxdex.MainActivity" to (TestAppActivityMeow::class.java as Class<RobloxActivity>)
    )
    override val apkFileName = "test.apk"
    override val apkLibraryFolderName = null // set to our app's own library path
}