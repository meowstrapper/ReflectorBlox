package com.drake.reflectorblox.dexactivity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import de.robv.android.xposed.XposedBridge

abstract class DexBasedActivity(
) : Activity() {
    lateinit var apkPath: String
    lateinit var libPath: String
    lateinit var activityClassName: String
    lateinit var applicationClassName: String

    private lateinit var dexClassLoader: BaseDexClassLoader
    private lateinit var activityClass: Class<*>
    private lateinit var activityInitializedClass: Activity
    private lateinit var pluginContext: Context
    private var applicationClass: Class<*>? = null
    private var applicationInitializedClass: Application? = null

    companion object {
        private const val TAG = "DexBasedActivity"
        private val propertiesToCopy: List<String> = listOf(
            "mActivityInfo",
            "mToken",
            "mAssistToken",
            "mShareableActivityToken",
            "mIntent",
            "mInstrumentation",
            "mMainThread",
            "mParent",
            "mContentCaptureManager",
            "mWindow",
            "mWindowManager",
            "mUiThread"
        )
    }


    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Disabling hidden api")
        XposedBridge.disableHiddenApiRestrictions()

        Log.d(TAG, "Loading dex class loader")
        dexClassLoader = DexClassLoader(
            apkPath,
            "meow", // ignored since api 26
            libPath,
            this.classLoader
        )

        Log.d(TAG, "Creating plugin context")
        pluginContext = createPluginContext()

        Log.d(TAG, "Initiliazing application")
        initApplication()

        Log.d(TAG, "Initializing activity (and onCreate)")
        initActivity(savedInstanceState)
    }

    @SuppressLint("MissingSuperCall")
    override fun onStart() {
        Log.d(TAG, "onStart()")
        getMethod(Activity::class.java, "onStart")
            .invoke(activityInitializedClass)
    }

    @SuppressLint("MissingSuperCall")
    override fun onResume() {
        Log.d(TAG, "onResume()")
        getMethod(Activity::class.java, "onResume")
            .invoke(activityInitializedClass)
    }

    @SuppressLint("MissingSuperCall")
    override fun onPause() {
        Log.d(TAG, "onPause()")
        getMethod(Activity::class.java, "onPause")
            .invoke(activityInitializedClass)
    }

    @SuppressLint("MissingSuperCall")
    override fun onStop() {
        Log.d(TAG, "onStop()")
        getMethod(Activity::class.java, "onStop")
            .invoke(activityInitializedClass)
    }

    @SuppressLint("MissingSuperCall")
    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        getMethod(Activity::class.java, "onDestroy")
            .invoke(activityInitializedClass)
    }

    private fun initActivity(savedInstanceState: Bundle?) {
        Activity()
        activityClass = dexClassLoader.loadClass(activityClassName)
        activityInitializedClass = activityClass.getDeclaredConstructor().newInstance() as Activity
        getField(Activity::class.java, "mApplication")!!
            .set(activityInitializedClass, applicationInitializedClass)
        getMethod(Activity::class.java, "attachBaseContext")
            .invoke(activityInitializedClass, pluginContext) // TODO attachBaseContext
        getMethod(Activity::class.java, "onCreate")
            .invoke(activityInitializedClass, savedInstanceState)
    }

    private fun initApplication() {
        applicationClass = dexClassLoader.loadClass(applicationClassName)
        if (applicationClass == null) {
            throw RuntimeException("Application class is null")
        }
        applicationInitializedClass = applicationClass!!.getDeclaredConstructor().newInstance() as Application
        getMethod(Application::class.java, "attach", Context::class.java)
            .invoke(applicationInitializedClass!!, pluginContext)
        applicationInitializedClass!!.onCreate()
    }

    private fun createPluginContext(): Context {
        val assetManager = AssetManager::class.java.newInstance()
        getMethod(AssetManager::class.java, "addAssetPath", String::class.java)
            .invoke(assetManager, apkPath)
        val superRes = this.resources
        val pluginResources =
            Resources(assetManager, superRes.displayMetrics, superRes.configuration)
        val wrapper = object : ContextWrapper(this) {
            override fun getClassLoader(): ClassLoader = dexClassLoader
            override fun getResources(): Resources = pluginResources
            override fun getAssets(): AssetManager = assetManager
            override fun getApplicationContext(): Context? = this // contextwrapper class
        }
        return wrapper
    }
}