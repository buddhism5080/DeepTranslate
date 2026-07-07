package io.github.kirby.deeptranslate.xposed.hook

import android.util.Log
import io.github.kirby.deeptranslate.xposed.ConfigManager
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

abstract class BaseHook {

    private val configChangeListener: () -> Unit = { onConfigChanged() }
    private var configListenerRegistered = false

    abstract fun getTag(): String
    abstract fun onInit(module: XposedModule, param: PackageLoadedParam)
    open fun onConfigChanged() {}

    protected fun ensureConfigManager(module: XposedModule) {
        ConfigManager.init(module)
        if (!configListenerRegistered) {
            ConfigManager.addChangeListener(configChangeListener)
            configListenerRegistered = true
        }
    }

    protected fun log(module: XposedModule, message: String) {
        if (ConfigManager.isDebugLogEnabled())
            module.log(Log.DEBUG, getTag(), message)
    }

    protected fun logWarn(module: XposedModule, message: String) {
        module.log(Log.WARN, getTag(), message)
    }

    protected fun logError(module: XposedModule, message: String) {
        module.log(Log.ERROR, getTag(), message)
    }

    fun init(module: XposedModule, param: PackageLoadedParam) {
        ensureConfigManager(module)
        log(module, "initializing for ${param.packageName}")
        try {
            onInit(module, param)
        } catch (e: Exception) {
            logError(module, "init failed: ${e.message}")
        }
    }
}
