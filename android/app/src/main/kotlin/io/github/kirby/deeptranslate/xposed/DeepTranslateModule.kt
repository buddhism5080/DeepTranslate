package io.github.kirby.deeptranslate.xposed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import io.github.kirby.deeptranslate.xposed.hook.LayoutHook
import io.github.kirby.deeptranslate.xposed.hook.ReactNativeHook
import io.github.kirby.deeptranslate.xposed.hook.TextViewHook
import io.github.kirby.deeptranslate.xposed.hook.WebViewHook
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModule

class DeepTranslateModule : XposedModule() {

    private var configManagerInitialized = false

    private val cacheClearReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                context?.let { ctx ->
                    TranslationCache.init(ctx)
                    val before = TranslationCache.count()
                    TranslationCache.clear()
                    Log.d("DeepTranslate", "cache cleared: was $before entries")
                    // 通知配置 App 缓存已清空
                    val notifyIntent = Intent("io.github.kirby.deeptranslate.CACHE_CLEARED").apply {
                        putExtra("package", context.packageName)
                        setPackage("io.github.kirby.deeptranslate")
                    }
                    context.sendBroadcast(notifyIntent)
                }
            } catch (e: Exception) {
                Log.e("DeepTranslate", "cache clear failed: ${e.message}")
            }
        }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        initializeConfigManager()

        val pkg = param.packageName

        // 注册广播接收器（运行时清空）
        try {
            val ctx = getContext(param.defaultClassLoader)
            if (ctx != null) {
                ctx.registerReceiver(
                    cacheClearReceiver,
                    IntentFilter("io.github.kirby.deeptranslate.CLEAR_CACHE"),
                    Context.RECEIVER_EXPORTED
                )
            }
        } catch (e: Exception) {
            log(Log.WARN, null, "failed to register cacheClearReceiver: ${e.message}")
        }

        if (pkg == "io.github.kirby.deeptranslate") return
        TextViewHook.init(this, param)
        LayoutHook.init(this, param)
        ReactNativeHook.init(this, param)
        WebViewHook.init(this, param)
    }

    private fun getContext(classLoader: ClassLoader): Context? {
        return try {
            val at = Class.forName("android.app.ActivityThread", false, classLoader)
            (at.getMethod("currentApplication").invoke(null) as? Context)
        } catch (e: Exception) { null }
    }

    private fun initializeConfigManager() {
        if (!configManagerInitialized) {
            ConfigManager.init(this)
            configManagerInitialized = true
        }
    }
}
