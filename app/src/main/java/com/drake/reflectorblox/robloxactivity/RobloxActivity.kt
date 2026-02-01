package com.drake.reflectorblox.robloxactivity

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.CallSuper
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File

/**
 * An abstract class designed to run a DEX file
 *
 * It uses the [DexClassLoader] class to load the APK's [Activity] class
 * and the [Application] class, and this host activity redirects its
 * lifecycle to the activity class.
 *
 * This is meant to be used for handling the Roblox APK
 *
 * @see DexClassLoader
 * @see Activity
 */
abstract class RobloxActivity : Activity() {
    companion object {
        private const val TAG = "RobloxActivity"
    }

    /**
     * The Activity's class full name it targets
     */
    abstract val activityClassName: String

    /**
     * It redirects an Activity called by [startActivity] into a [DexClassLoader]
     *
     * @param String The Activity's class full name to target
     * @param Class The [RobloxActivity] class reference where it will be redirected to
     */
    open val activityRedirections: Map<String, Class<RobloxActivity>> = emptyMap()

    /**
     * The package name of the APK retrieved by [getPackageName]
     */
    open val appPackageName: String = "com.roblox.client"

    /**
     * The Application's class full name it targets
     */
    open val applicationClassName: String = "com.roblox.client.RobloxApplication"

    /**
     * The APK File name inside the folder from [getFilesDir]
     */
    open val apkFileName: String = "roblox.apk"

    /**
     * The APK's library folder inside the folder from [getFilesDir]
     */
    open val apkLibraryFolderName: String? = "robloxlibs"

    /**
     * APK path used by [DexClassLoader]
     */
    private lateinit var robloxApkPath: String

    /**
     * APK's library path used by [DexClassLoader]
     */
    private lateinit var robloxLibPath: String

    /**
     * The [DexClassLoader] initialized and used by [Context.getClassLoader]
     */
    private lateinit var dexClassLoader: BaseDexClassLoader

    /**
     * A Class reference to [activityClassName]
     */
    private lateinit var activityClass: Class<Activity>

    /**
     * An [Activity] reference loaded by the [activityClass]
     */
    private lateinit var activityInitializedClass: Activity

    /**
     * A Class reference to [applicationClassName]
     */
    private lateinit var applicationClass: Class<Application>

    /**
     * An [Application] reference loaded by the [applicationClass]
     */
    private lateinit var applicationInitializedClass: Application

    /**
     * The [AssetManager] used by the APK
     */
    private lateinit var assetManager: AssetManager

    /**
     * A [ContextWrapper] created by [createPluginContext]
     */
    private lateinit var pluginContext: ContextWrapper

    /**
     * A list of fields to copy from the host's [Activity] class to the APK's one
     */
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

    /**
     * Initializes the APK's [Activity] and [Application] class
     */
    @CallSuper
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

        try {
            Log.d(TAG, "Loading APK DEX")
            dexClassLoader = DexClassLoader(
                robloxApkPath,
                "meow", // ignored since api 26
                robloxLibPath,
                this.classLoader
            )

            Log.d(TAG, "Creating plugin context")
            pluginContext = createPluginContext()

            // TODO: Don't initialize the Application class every time a new activity appears
            Log.d(TAG, "Initializing application")
            applicationClass = dexClassLoader.loadClass(applicationClassName) as Class<Application>
            applicationInitializedClass =
                getConstructor(applicationClass).newInstance() as Application

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
                                param.args[0] = this@RobloxActivity
                                param.args[1] = redirectTo
                            }
                        }
                    }
                }
            })

            Log.d(TAG, "Disabling Roblox's Crashpad Handler")
            try {
                XposedBridge.hookMethod(
                    dexClassLoader.loadClass("com.roblox.client.analytics.CrashpadHandler")
                        .getDeclaredMethod("main", Array<String>::class.java),
                    object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            Log.d(TAG, "son im crine 😭")
                            return null
                        }

                    }
                )
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "Roblox's CrashpadHandler method was not found. $e")
            } catch (e: Exception) {
                Log.d(TAG, "Something went wrong while trying to hook the CrashpadHandler: $e")
            }

            Log.d(TAG, "Initializing activity")
            activityClass = dexClassLoader.loadClass(activityClassName) as Class<Activity>
            activityInitializedClass = getConstructor(activityClass).newInstance() as Activity

            getMethod(
                clazz = Activity::class.java,
                method = "attachBaseContext",
                Context::class.java
            ).invoke(activityInitializedClass, pluginContext)

            try {
                getFields(Activity::class.java).forEach { field ->
                    if (fieldsToCopy.contains(field.name)) {
                        Log.d(TAG, "copying $field.name")
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
        } catch (e: Exception) {
            Log.e(TAG, "Error while trying to initialize activity: ${e.message}")
            Toast.makeText(
                this,
                "Error while trying to initialize activity, check logcat.",
                Toast.LENGTH_SHORT
            ).show()
            this.finish()
        }
    }

    @CallSuper
    override fun onStart() {
        Log.d(TAG, "onStart()")
        super.onStart()
        if (::activityInitializedClass.isInitialized) {
            Log.d(TAG, "Calling APK's onStart()")
            getMethod(
                clazz = Activity::class.java,
                method = "onStart"
            ).invoke(activityInitializedClass)
        }
    }

    @CallSuper
    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        if (::activityInitializedClass.isInitialized) {
            Log.d(TAG, "Calling APK's onResume()")
            getMethod(
                clazz = Activity::class.java,
                method = "onResume"
            ).invoke(activityInitializedClass)
        }
    }

    @CallSuper
    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()
        if (::activityInitializedClass.isInitialized) {
            Log.d(TAG, "Calling APK's onPause()")
            getMethod(
                clazz = Activity::class.java,
                method = "onPause"
            ).invoke(activityInitializedClass)
        }
    }

    @CallSuper
    override fun onStop() {
        Log.d(TAG, "onStop()")
        super.onStop()
        if (::activityInitializedClass.isInitialized) {
            Log.d(TAG, "Calling APK's onStop()")
            getMethod(
                clazz = Activity::class.java,
                method = "onStop"
            ).invoke(activityInitializedClass)
        }
    }

    @CallSuper
    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
        if (::activityInitializedClass.isInitialized) {
            Log.d(TAG, "Calling APK's onDestroy()")
            getMethod(
                clazz = Activity::class.java,
                method = "onDestroy"
            ).invoke(activityInitializedClass)
        }
    }

    /**
     * Creates a [ContextWrapper] class with overwritten methods
     * required by the [Application] and [Activity] class
     *
     * @return [ContextWrapper]
     */
    private fun createPluginContext(): ContextWrapper {
        assetManager = getConstructor(AssetManager::class.java).newInstance() as AssetManager
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
            override fun getPackageName(): String = appPackageName
            override fun getResources(): Resources = pluginResources
        }
    }
}