package io.github.kirby.deeptranslate

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

/**
 * 自定义 Application，负责将 Flutter 端写入的 SharedPreferences
 * 镜像同步到 LSPosed 的 RemotePreferences，使 Hook 进程能读到最新配置。
 *
 * 架构抄自 HyperIsland：
 * - App 端通过 XposedServiceHelper 获取 XposedService
 * - 配置拆为 core + shards，避免 Binder 事务超限
 */
class XposedPrefsSyncApp : Application(), XposedServiceHelper.OnServiceListener {

    private val flutterPrefs: SharedPreferences by lazy {
        getSharedPreferences(FLUTTER_PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    private var xposedService: XposedService? = null

    private val flutterPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        syncKeyToRemote(prefs, key)
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
        flutterPrefs.registerOnSharedPreferenceChangeListener(flutterPrefsListener)
    }

    override fun onTerminate() {
        flutterPrefs.unregisterOnSharedPreferenceChangeListener(flutterPrefsListener)
        xposedService = null
        ServiceState.markNotReady()
        super.onTerminate()
    }

    // ── XposedService 回调 ────────────────────────────────────────────────────

    override fun onServiceBind(service: XposedService) {
        xposedService = service
        ServiceState.markReady(service.apiVersion, service.frameworkName, service.frameworkVersion)
        Log.d(TAG, "XposedService bound, syncing sharded prefs")
        syncAllToRemote(service)
        ServiceState.notifyReady()
    }

    override fun onServiceDied(service: XposedService) {
        xposedService = null
        ServiceState.markNotReady()
        Log.d(TAG, "XposedService died")
    }

    // ── 同步实现 ──────────────────────────────────────────────────────────────

    private fun syncKeyToRemote(prefs: SharedPreferences, key: String?) {
        val service = xposedService ?: return
        syncToRemote(service, prefs, key)
    }

    private fun syncAllToRemote(service: XposedService) {
        syncToRemote(service, flutterPrefs, key = null)
    }

    private fun syncToRemote(service: XposedService, sourcePrefs: SharedPreferences, key: String?) {
        try {
            if (key == null) {
                writeAllSharded(service, sourcePrefs)
            } else {
                if (!shouldSyncKey(key)) return
                val remote = service.getRemotePreferences(remotePrefsNameForKey(key))
                val editor = remote.edit() ?: return
                writeValue(editor, key, sourcePrefs.all[key])
                editor.apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncToRemote failed: ${e.message}")
        }
    }

    fun getFrameworkInfo(): Map<String, Any> {
        val service = xposedService ?: throw IllegalStateException("XposedService is not ready")
        return mapOf(
            "apiVersion" to service.apiVersion,
            "frameworkName" to service.frameworkName,
            "frameworkVersion" to service.frameworkVersion,
            "scope" to service.scope
        )
    }

    private fun writeAllSharded(service: XposedService, src: SharedPreferences) {
        clearAllRemotePrefs(service)

        val grouped = src.all
            .filterKeys { shouldSyncKey(it) }
            .entries
            .groupBy { remotePrefsNameForKey(it.key) }

        for ((prefsName, entries) in grouped) {
            val remote = service.getRemotePreferences(prefsName)
            var editor = remote.edit() ?: continue
            for ((key, value) in entries) {
                writeValue(editor, key, value)
            }
            editor.apply()
            Log.d(TAG, "synced ${entries.size} keys to $prefsName")
        }
    }

    private fun clearAllRemotePrefs(service: XposedService) {
        service.getRemotePreferences(REMOTE_PREFS_CORE).edit()?.clear()?.apply()
        for (index in 0 until SHARD_COUNT) {
            service.getRemotePreferences("$REMOTE_PREFS_SHARD_PREFIX$index").edit()?.clear()?.apply()
        }
    }

    private fun writeValue(editor: SharedPreferences.Editor, key: String, value: Any?) {
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int     -> editor.putInt(key, value)
            is Long    -> editor.putLong(key, value)
            is Float   -> editor.putFloat(key, value)
            is String  -> editor.putString(key, value)
            is Set<*>  -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            null       -> editor.remove(key)
        }
    }

    private fun shouldSyncKey(key: String): Boolean {
        if (!key.startsWith(FLUTTER_KEY_PREFIX)) return false
        val rawKey = key.removePrefix(FLUTTER_KEY_PREFIX)
        if (!rawKey.startsWith("pref_")) return false
        return rawKey != "pref_onboarding_completed"
    }

    private fun remotePrefsNameForKey(key: String): String {
        val rawKey = key.removePrefix(FLUTTER_KEY_PREFIX)
        return if (isCoreKey(rawKey)) {
            REMOTE_PREFS_CORE
        } else {
            "$REMOTE_PREFS_SHARD_PREFIX${shardForKey(key)}"
        }
    }

    private fun isCoreKey(rawKey: String): Boolean = rawKey in CORE_PREF_KEYS

    private fun shardForKey(key: String): Int =
        (key.hashCode() and Int.MAX_VALUE) % SHARD_COUNT

    companion object {
        private const val TAG = "DeepTranslate[App]"
        private const val FLUTTER_PREFS_NAME = "FlutterSharedPreferences"
        private const val FLUTTER_KEY_PREFIX = "flutter."
        const val REMOTE_PREFS_CORE = "DeepTranslateXposedCore"
        const val REMOTE_PREFS_SHARD_PREFIX = "DeepTranslateXposedShard"
        const val SHARD_COUNT = 32

        private val CORE_PREF_KEYS = setOf(
            "pref_translation_enabled",
            "pref_ai_url",
            "pref_ai_api_key",
            "pref_ai_model",
            "pref_ai_prompt",
            "pref_ai_target_lang",
            "pref_ai_timeout",
            "pref_ai_temperature",
            "pref_ai_max_tokens",
            "pref_batch_size",
            "pref_batch_window_ms",
            "pref_cache_enabled",
            "pref_concurrency",
            "pref_max_paragraphs",
            "pref_max_chars",
            "pref_bilingual",
            "pref_retry_count",
            "pref_fallback_url",
            "pref_fallback_api_key",
            "pref_fallback_model",
            "pref_translate_toast",
            "pref_app_whitelist",
            "pref_hook_layout",
            "pref_hook_webview",
            "pref_hook_compose",
            "pref_theme_mode",
            "pref_theme_seed_color",
            "pref_blur_bars",
            "pref_debug_log",
            "pref_onboarding_completed"
        )

        private object ServiceState {
            @Volatile private var serviceReady = false
            @Volatile private var apiVersion: Int = 0
            @Volatile private var frameworkName: String = ""
            @Volatile private var frameworkVersion: String = ""
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            private val serviceReadyLock = Object()

            fun isReady(): Boolean = serviceReady

            fun getApiVersion(): Int = apiVersion

            fun getFrameworkName(): String = frameworkName

            fun getFrameworkVersion(): String = frameworkVersion

            fun markReady(newApiVersion: Int, newFrameworkName: String, newFrameworkVersion: String) {
                apiVersion = newApiVersion
                frameworkName = newFrameworkName
                frameworkVersion = newFrameworkVersion
                serviceReady = true
            }

            fun markNotReady() {
                serviceReady = false
                apiVersion = 0
                frameworkName = ""
                frameworkVersion = ""
            }

            fun notifyReady() {
                synchronized(serviceReadyLock) { serviceReadyLock.notifyAll() }
            }

            fun awaitReady(timeoutMs: Long): Boolean {
                if (isReady()) return true
                synchronized(serviceReadyLock) {
                    if (!isReady()) {
                        try {
                            serviceReadyLock.wait(timeoutMs)
                        } catch (_: InterruptedException) {}
                    }
                }
                return isReady()
            }
        }

        fun isReady(): Boolean = ServiceState.isReady()
        fun getApiVersion(): Int = ServiceState.getApiVersion()
        fun getFrameworkName(): String = ServiceState.getFrameworkName()
        fun getFrameworkVersion(): String = ServiceState.getFrameworkVersion()
        fun awaitReady(timeoutMs: Long = 1500): Boolean = ServiceState.awaitReady(timeoutMs)
    }
}
