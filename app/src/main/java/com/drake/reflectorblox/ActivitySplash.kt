package com.drake.reflectorblox

import android.os.Bundle
import com.drake.reflectorblox.robloxactivity.RobloxActivity

class ActivitySplash : RobloxActivity() {
    override val activityClassName = "com.roblox.client.ActivitySplash"
    override val activityRedirections = mapOf(
        "com.roblox.client.ActivityNativeMain" to (ActivityNativeMain::class.java as Class<RobloxActivity>)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}