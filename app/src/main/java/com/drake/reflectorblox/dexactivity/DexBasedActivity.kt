package com.drake.reflectorblox.dexactivity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.ktor.util.reflect.instanceOf
import org.lsposed.hiddenapibypass.HiddenApiBypass

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

    private val intentHooks: MutableMap<String, Class<DexBasedActivity>> = mutableMapOf()

    companion object {
        private const val TAG = "DexBasedActivity"
        private val propertiesToCopy: List<String> = listOf(
            "mFragments",
            "mWindow",
            "mUiThread",
            "mMainThread",
            "mInstrumentation",
            "mToken",
            "mAssistToken",
            "mShareableActivityToken",
            "mIdent",
            "mApplication",
            "mIntent",
            "mReferrer",
            "mComponent",
            "mTitle",
            "mParent",
            "mEmbeddedID",
            "mLastNonConfigurationInstances",
            "mWindowManager",
            "mCurrentConfig",
            "mActivityInfo"
        )
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
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

            Log.d(TAG, "Hooking intent constructor")
            hookIntent()

            Log.d(TAG, "Initializing activity (and onCreate)")
            initActivity(savedInstanceState)
        } catch (e: Exception) {
            Log.e(TAG, "Error while trying to initialize activity: ${e.message}")
            Toast.makeText(this, "Error while trying to initialize activity, check logs.", Toast.LENGTH_SHORT).show()
            this.finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingSuperCall")
    override fun onStart() {
        Log.d(TAG, "onStart()")
        super.onStart()
        HiddenApiBypass.invoke(Activity::class.java, activityInitializedClass, "onStart")
//        getMethod(Activity::class.java, "onStart")
//            .invoke(activityInitializedClass)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingSuperCall")
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
        HiddenApiBypass.invoke(Activity::class.java, activityInitializedClass, "onResume")
//        getMethod(Activity::class.java, "onResume")
//            .invoke(activityInitializedClass)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingSuperCall")
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause()")
        HiddenApiBypass.invoke(Activity::class.java, activityInitializedClass, "onPause")
//        getMethod(Activity::class.java, "onPause")
//            .invoke(activityInitializedClass)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingSuperCall")
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop()")
        HiddenApiBypass.invoke(Activity::class.java, activityInitializedClass, "onStop")
//        getMethod(Activity::class.java, "onStop")
//            .invoke(activityInitializedClass)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingSuperCall")
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
        HiddenApiBypass.invoke(Activity::class.java, activityInitializedClass, "onDestroy")
//        getMethod(Activity::class.java, "onDestroy")
//            .invoke(activityInitializedClass)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun initActivity(savedInstanceState: Bundle?) {
        activityClass = dexClassLoader.loadClass(activityClassName)
        activityInitializedClass = activityClass.getDeclaredConstructor().newInstance() as Activity

        Log.d(TAG, "Calling activity's attachBaseContext")
        HiddenApiBypass.invoke(Activity::class.java, activityInitializedClass, "attachBaseContext", pluginContext)
//        getMethod(Activity::class.java, "attachBaseContext", Context::class.java)
//            .invoke(activityInitializedClass, getField(Activity::class.java, "mBase").get(this))
//        listOf(
//            "mFragments",
//            "mWindow",
//            "mUiThread",
//            "mMainThread",
//            "mInstrumentation",
//            "mToken",
//            "mAssistToken",
//            "mShareableActivityToken",
//            "mIdent",
//            "mApplication",
//            "mIntent",
//            "mReferrer",
//            "mComponent",
//            "mTitle",
//            "mParent",
//            "mEmbeddedID",
//            "mLastNonConfigurationInstances",
//            "mWindowManager",
//            "mCurrentConfig"
//        ).forEach {
//            try {
//                getField(Activity::class.java, it).let { field ->
//                    field.set(activityInitializedClass, field.get(this))
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "An error occured while trying to copy the field '$it': $e")
//            }
//        }

        try {
            HiddenApiBypass.getInstanceFields(Activity::class.java).forEach { field ->
                if (field.name in propertiesToCopy) {
                    Log.d(TAG, "Copying $field.name")
                    field.isAccessible = true
                    field.set(activityInitializedClass, field.get(this))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occured while trying to copy the properties: $e")
        }
        HiddenApiBypass.getInstanceFields(Activity::class.java).forEach { field ->
            if (field.name == "mApplication") {
                field.isAccessible = true
                field.set(activityInitializedClass, applicationInitializedClass)
            }
        }
        Log.d(TAG, "Calling activity's onCreate")
        HiddenApiBypass.getInstanceFields(Activity::class.java).forEach { field ->
            if (field.name == "mInstrumentation") {
                field.isAccessible = true
                val instrumentation: Instrumentation = field.get(this) as Instrumentation
                instrumentation.callActivityOnCreate(activityInitializedClass, savedInstanceState)
            }
        }
        //HiddenApiBypass.invoke(Activity::class.java, activityInitializedClass, "onCreate", savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun initApplication() {
        applicationClass = dexClassLoader.loadClass(applicationClassName)
        if (applicationClass == null) {
            throw RuntimeException("Application class is null")
        }
        applicationInitializedClass = applicationClass!!.getDeclaredConstructor().newInstance() as Application
        HiddenApiBypass.invoke(Application::class.java, applicationInitializedClass!!, "attach", pluginContext)
//        getMethod(Application::class.java, "attach", Context::class.java)
//            .invoke(applicationInitializedClass!!, pluginContext)
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

    private fun hookIntent() {
        XposedBridge.hookAllConstructors(Intent::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.args[0]?.let {
                    if (it is Context) {
                        val activityClassName = (param.args[1] as Class<*>).name
                        if (activityClassName in intentHooks) {
                            Log.d(TAG, "Replacing intent $activityClassName with ${intentHooks[activityClassName::class.qualifiedName]}")
                            param.args[1] = intentHooks[activityClassName]!!
                        }
                    }
                }
            }
        })
    }

    fun hookActivity(activityToHook: String, activity: Class<DexBasedActivity>) {
        if (activityToHook in intentHooks) {
            return
        }

        Log.d(TAG, "Hooking $activityToHook to ${activity.name}")
        intentHooks[activityToHook] = activity
    }
}