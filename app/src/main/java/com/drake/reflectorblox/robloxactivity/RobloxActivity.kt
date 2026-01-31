package com.drake.reflectorblox.robloxactivity

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.ktor.util.reflect.instanceOf
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File

abstract class RobloxActivity : Activity() {
    companion object {
        private const val TAG = "RobloxActivity"
    }

    abstract val activityClassName: String

    open val activityRedirections: Map<String, Class<RobloxActivity>> = emptyMap()
    open val appPackageName: String = "com.roblox.client"
    open val applicationClassName: String = "com.roblox.client.RobloxApplication"
    open val apkFileName: String = "roblox.apk"
    open val apkLibraryFolderName: String? = "robloxlibs"

    private lateinit var robloxApkPath: String
    private lateinit var robloxLibPath: String

    private lateinit var dexClassLoader: BaseDexClassLoader

    private lateinit var activityClass: Class<Activity>
    private lateinit var activityInitializedClass: Activity

    private lateinit var applicationClass: Class<Application>
    private lateinit var applicationInitializedClass: Application

    private lateinit var assetManager: AssetManager
    private lateinit var pluginContext: Context

    private val fieldsToCopy: List<String> = listOf(
        "mActivityInfo",
        "mApplication",
        "mAssistToken",
        "mComponent",
        "mCurrentConfig",
        "mEmbeddedID",
        "mFragments",
        "mIdent",
        "mInstrumentation",
        "mIntent",
        "mLastNonConfigurationInstances",
        "mMainThread",
        "mParent",
        "mReferrer",
        "mShareableActivityToken",
        "mTitle",
        "mToken",
        "mUiThread",
        "mWindow",
        "mWindowManager"
    ) // TODO: remove redundant fields

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filesDir = this.filesDir

        val apkFile = File(filesDir, apkFileName)
        if (!apkFile.exists()) {
            throw RuntimeException("APK doesn't exist")
        }
        apkFile.setReadOnly()
        robloxApkPath = apkFile.absolutePath

        if (apkLibraryFolderName != null) {
            val libPath = File(filesDir, apkLibraryFolderName!!)
            if (!apkFile.exists()) {
                throw RuntimeException("Library path doesn't exist")
            }
            robloxLibPath = libPath.absolutePath
        } else {
            robloxLibPath = this.applicationContext.applicationInfo.nativeLibraryDir
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.d(TAG, "Disabling hidden api bypass")
            HiddenApiBypass.addHiddenApiExemptions("L")
        }

//        try {
        Log.d(TAG, "Loading APK DEX")
        dexClassLoader = DexClassLoader(
            robloxApkPath,
            "meow", // ignored since api 26
            robloxLibPath,
            this.classLoader
        )

        Log.d(TAG, "Creating plugin context")
        pluginContext = createPluginContext()

        Log.d(TAG, "Initializing application")
        applicationClass = dexClassLoader.loadClass(applicationClassName) as Class<Application>
        applicationInitializedClass = getConstructor(applicationClass).newInstance() as Application

        getMethod(
            clazz = Application::class.java,
            method = "attach",
            Context::class.java
        ).invoke(applicationInitializedClass, pluginContext)

        applicationInitializedClass.onCreate()

        Log.d(TAG, "Hooking Intent constructor")
        XposedBridge.hookAllConstructors(Intent::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.args[0]?.let {
                    if (it is Context) {
                        val activityClassName = (param.args[1] as Class<*>).name
                        if (activityRedirections.contains(activityClassName)) {
                            val redirectTo = activityRedirections[activityClassName]!!
                            Log.d(TAG, "Redirecting $activityClassName to ${redirectTo.name}")
                            param.args[1] = redirectTo
                        }
                    }
                }
            }
        })
        Log.d(TAG, "Initializing activity")
        activityClass = dexClassLoader.loadClass(activityClassName) as Class<Activity>
        activityInitializedClass = getConstructor(activityClass).newInstance() as Activity

        getMethod(
            clazz = Activity::class.java,
            method = "attachBaseContext",
            Context::class.java
        ).invoke(activityInitializedClass, pluginContext)

        try {
//            getFields(activityClass).forEach { field ->
//                if (fieldsToCopy.contains(field.name)) {
//                    Log.d(TAG, "copying $field.name")
//                    field.set(activityInitializedClass, field.get(this))
//                }
//            }
            Activity::class.java.declaredFields.forEach { field ->
                if (fieldsToCopy.contains(field.name)) {
                    Log.d(TAG, "copying $field.name")
                    field.isAccessible = true
                    field.set(activityInitializedClass, field.get(this))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occured while trying to copy the properties: $e")
        }

        getField(
            clazz = Activity::class.java,
            field = "mApplication"
        )!!.set(activityInitializedClass, applicationInitializedClass)

        Log.d(TAG, "Calling activity's onCreate")
        (getField(
            clazz = Activity::class.java,
            field = "mInstrumentation"
        )!!.get(this) as Instrumentation).callActivityOnCreate(
            activityInitializedClass,
            savedInstanceState
        )
//        } catch (e: Exception) {
//            Log.e(TAG, "Error while trying to initialize activity: ${e.message}")
//            Toast.makeText(
//                this,
//                "Error while trying to initialize activity, check logcat.",
//                Toast.LENGTH_SHORT
//            ).show()
//            this.finish()
//        }
    }

    override fun onStart() {
        Log.d(TAG, "onStart()")
        super.onStart()
        getMethod(
            clazz = Activity::class.java,
            method = "onStart"
        ).invoke(activityInitializedClass)
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        getMethod(
            clazz = Activity::class.java,
            method = "onResume"
        ).invoke(activityInitializedClass)
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()
        getMethod(
            clazz = Activity::class.java,
            method = "onPause"
        ).invoke(activityInitializedClass)
    }

    override fun onStop() {
        Log.d(TAG, "onStop()")
        super.onStop()
        getMethod(
            clazz = Activity::class.java,
            method = "onStop"
        ).invoke(activityInitializedClass)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
        getMethod(
            clazz = Activity::class.java,
            method = "onDestroy"
        ).invoke(activityInitializedClass)
    }

    private fun createPluginContext(): Context {
        //assetManager = getConstructor(AssetManager::class.java).newInstance() as AssetManager
        assetManager = AssetManager::class.java.newInstance()
        getMethod(
            clazz = AssetManager::class.java,
            method = "addAssetPath",
            String::class.java
        ).invoke(assetManager, robloxApkPath)

        val superRes = this.resources
        val pluginResources = Resources(
            assetManager,
            superRes.displayMetrics,
            superRes.configuration
        )

        val applicationInfo = ApplicationInfo(this.applicationContext.applicationInfo).apply {
            this.sourceDir = robloxApkPath
            this.nativeLibraryDir = robloxLibPath
            this.className = applicationClassName
        }

        return object : ContextWrapper(this) {
            override fun getApplicationContext(): Context? = this // contextwrapper class
            override fun getApplicationInfo(): ApplicationInfo = applicationInfo
            override fun getAssets(): AssetManager = assetManager
            override fun getClassLoader(): ClassLoader = dexClassLoader
            //override fun getPackageName(): String = appPackageName // TODO: Fix startActivity looking at the overwritten package name instead of the app's one
            override fun getResources(): Resources = pluginResources
        }
    }
}