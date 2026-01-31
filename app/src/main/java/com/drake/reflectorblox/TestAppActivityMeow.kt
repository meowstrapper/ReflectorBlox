package com.drake.reflectorblox

import com.drake.reflectorblox.robloxactivity.RobloxActivity

class TestAppActivityMeow : RobloxActivity() {
    override val applicationClassName = "com.drake.testappforrobloxdex.Application"
    override val activityClassName = "com.drake.testappforrobloxdex.MainActivity"
    override val apkFileName = "test.apk"
    override val apkLibraryFolderName = null // set to our app's own library path
}