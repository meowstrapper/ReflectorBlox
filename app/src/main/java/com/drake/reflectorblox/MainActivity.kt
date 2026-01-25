package com.drake.reflectorblox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drake.reflectorblox.logger.AndroidLogger
import com.drake.reflectorblox.roblox.RobloxRepository
import com.drake.reflectorblox.ui.theme.ReflectorBloxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    lateinit var robloxHandler: RobloxRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        robloxHandler = RobloxRepository(AndroidLogger())
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ReflectorBloxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        val scope = rememberCoroutineScope()
                        var downloadingRobloxAPK by remember { mutableStateOf(false) }
                        var textStatus by remember { mutableStateOf("click to download roblox apk") }

                        Text(textStatus)

                        Button(
                            enabled = !downloadingRobloxAPK,
                            onClick = {
                                if (!downloadingRobloxAPK) {
                                    val filesDir = this@MainActivity.filesDir
                                    scope.launch(Dispatchers.IO) {
                                        downloadingRobloxAPK = true
                                        textStatus = "downloading roblosx"
                                        robloxHandler.downloadLatestRobloxRelease(
                                            "arm64_v8a",
                                            File(filesDir, "base.apk"),
                                            File(filesDir, "robloxlibs")
                                        )
                                        downloadingRobloxAPK = false
                                        textStatus = "done downloading roblox"
                                    }
                                }
                            }
                        ) { Text("Install Roblox") }

                        Button(
                            onClick = {
//                                val intent = Intent(this@MainActivity, RobloxActivity::class.java)
//                                startActivity(intent)
                                scope.launch(Dispatchers.IO) {
                                    val filesDir = this@MainActivity.filesDir
                                    if (!File(filesDir, "app-release.apk").exists()) {
                                        val assetManager = this@MainActivity.assets
                                        assetManager.open("app-release.apk").use { inputStream ->
                                            FileOutputStream(File(filesDir, "app-release.apk")).use { outputStream ->
                                                inputStream.copyTo(outputStream)
                                            }
                                        }
                                    }
                                    val intent = Intent(this@MainActivity, TestActivity::class.java)
                                    this@MainActivity.startActivity(intent)
                                    
                                }
                            }
                        ) { Text("Launch Test") }
                    }
                }
            }
        }
    }
}