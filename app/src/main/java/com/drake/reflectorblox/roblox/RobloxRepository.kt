package com.drake.reflectorblox.roblox

import com.drake.reflectorblox.logger.Logger
import com.drake.reflectorblox.roblox.models.GithubRelease
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class RobloxRepository(
    private val customLogger: Logger
) {
    companion object {
        private const val TAG = "RobloxHandler"
        private const val APK_REPO = "Roblox-DeployHistory-Updates/android-runner"
    }
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    customLogger.d(TAG, message)
                }
            }
            level = LogLevel.HEADERS
        }
    }

    private suspend fun downloadFile(
        url: String,
        output: File
    ) {
        //httpClient.get(url).bodyAsChannel().copyAndClose(output.writeChannel())
        httpClient.prepareGet(url).execute { httpResponse ->
            val channel = httpResponse.bodyAsChannel()
            output.outputStream().use { outputStream ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.exhausted()) {
                        val bytes = packet.readByteArray()
                        outputStream.write(bytes)
                    }
                }
            }
        }
    }

    suspend fun fetchLatestRobloxRelease(): GithubRelease = httpClient.get(
        "https://api.github.com/repos/$APK_REPO/releases/latest"
    ).body<GithubRelease>()

    suspend fun downloadLatestRobloxRelease(
        arch: String,
        downloadBaseApkPath: File,
        extractSplitApkPath: File
    ) {
        val latestRelease = fetchLatestRobloxRelease()
        var baseApkUrl: String? = null
        var splitApkUrl: String? = null
        val writeSplitApkTo = File(extractSplitApkPath, "split.apk")

        if (!extractSplitApkPath.exists()) extractSplitApkPath.mkdirs()

        latestRelease.assets.forEach {
            if (it.name.startsWith("com.roblox.client")) {
                if (it.name.contains("config") && it.name.contains(arch)) {
                    splitApkUrl = it.browserDownloadUrl
                } else if (!it.name.contains("config")) {
                    baseApkUrl = it.browserDownloadUrl
                }
            }
        }
        baseApkUrl.let {
            customLogger.d(TAG, "Base APK Url is $baseApkUrl")
            customLogger.d(TAG, "Downloading Base APK")
            downloadFile(it!!, downloadBaseApkPath)
        }
        splitApkUrl.let {
            customLogger.d(TAG, "Split APK Url is $splitApkUrl")

            customLogger.d(TAG, "Downloading Split APK")
            downloadFile(it!!, writeSplitApkTo)

            customLogger.d(TAG, "Extracting Split APK")
            ZipFile(writeSplitApkTo).use { zipFile ->
                zipFile.entries().asSequence().forEach { entry ->
                    if (entry.name.startsWith("lib") && entry.name.endsWith(".so")) {
                        zipFile.getInputStream(entry).use { input ->
                            FileOutputStream(File(extractSplitApkPath, File(entry.name).name)).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }

            customLogger.d(TAG, "Clearing up Split APK")
            writeSplitApkTo.delete()
        }
    }
}