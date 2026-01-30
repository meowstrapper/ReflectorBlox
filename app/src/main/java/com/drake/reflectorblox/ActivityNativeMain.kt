package com.drake.reflectorblox

import android.os.Bundle
import com.drake.reflectorblox.robloxactivity.RobloxActivity

class ActivityNativeMain : RobloxActivity() {
    override val activityClassName = "com.roblox.client.ActivityNativeMain"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}