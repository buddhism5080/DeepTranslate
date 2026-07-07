package io.github.kirby.deeptranslate.xposed

import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.api.XposedModule

/**
 * 基于 RemotePreferences 的配置管理器。
 * Flutter 端写入的 SharedPreferences 通过 XposedService 镜像到 Hook 进程。
 * 配置拆为 core + shards，避免 Binder 事务超限。
 */
object ConfigManager {

    private const val TAG = "DeepTranslate[Config]"
    private const val FLUTTER_KEY_PREFIX = "flutter."
    private const val PREFS_CORE = "DeepTranslateXposedCore"
    private const val PREFS_SHARD_PREFIX = "DeepTranslateXposedShard"
    private const val SHARD_COUNT = 32
    private const val DOUBLE_PREFIX_ENCODED = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBEb3VibGUu"

    @Volatile private var corePrefs: SharedPreferences? = null
    @Volatile private var initialized = false
    @Volatile private var module: XposedModule? = null

    private val shardPrefs = arrayOfNulls<SharedPreferences>(SHARD_COUNT)
    private val changeListeners = mutableListOf<() -> Unit>()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        module?.log(Log.DEBUG, TAG, "prefs changed: key=$key")
        notifyListeners()
    }

    @Synchronized
    fun init(module: XposedModule) {
        if (initialized) return
        try {
            val p = module.getRemotePreferences(PREFS_CORE)
            p.registerOnSharedPreferenceChangeListener(prefsListener)
            corePrefs = p
            this.module = module
            initialized = true
            module.log(Log.INFO, TAG, "remote prefs '$PREFS_CORE' loaded")
            notifyListeners()
        } catch (e: UnsupportedOperationException) {
            module.log(Log.WARN, TAG, "init failed: embedded framework, remote prefs unavailable")
            initialized = true
        } catch (e: Throwable) {
            module.log(Log.ERROR, TAG, "init failed: ${e.message}")
            initialized = true
        }
    }

    @Synchronized
    fun addChangeListener(listener: () -> Unit) {
        changeListeners += listener
    }

    // ── 类型化读取 ──────────────────────────────────────────────────────────

    fun getBoolean(key: String, default: Boolean): Boolean =
        try { prefsForKey(key)?.getBoolean(fk(key), default) ?: default }
        catch (_: ClassCastException) { default }

    fun getString(key: String, default: String = ""): String =
        try { prefsForKey(key)?.getString(fk(key), default) ?: default }
        catch (_: ClassCastException) { default }

    fun getInt(key: String, default: Int): Int =
        try { prefsForKey(key)?.getLong(fk(key), default.toLong())?.toInt() ?: default }
        catch (_: ClassCastException) {
            try { prefsForKey(key)?.getInt(fk(key), default) ?: default }
            catch (_: ClassCastException) { default }
        }

    fun getDouble(key: String, default: Double): Double {
        val raw = try { prefsForKey(key)?.getString(fk(key), null) } catch (_: Throwable) { null }
            ?: return default
        return try {
            if (raw.startsWith(DOUBLE_PREFIX_ENCODED)) {
                raw.substring(DOUBLE_PREFIX_ENCODED.length).trim().toDoubleOrNull() ?: default
            } else {
                raw.toDoubleOrNull() ?: default
            }
        } catch (_: Throwable) { default }
    }

    fun contains(key: String): Boolean =
        prefsForKey(key)?.contains(fk(key)) ?: false

    fun isDebugLogEnabled(): Boolean = getBoolean("pref_debug_log", false)

    fun module(): XposedModule? = module

    // ── 便捷配置读取 ────────────────────────────────────────────────────────

    fun isTranslationEnabled(): Boolean = getBoolean("pref_translation_enabled", false)
    fun getAiUrl(): String = getString("pref_ai_url", "https://api.deepseek.com/v1/chat/completions")
    fun getAiApiKey(): String = getString("pref_ai_api_key", "")
    fun getAiModel(): String = getString("pref_ai_model", "deepseek-v4-flash")
    fun getAiPrompt(): String = getString("pref_ai_prompt", "")
    fun getTargetLang(): String = getString("pref_ai_target_lang", "中文")
    fun getAiTimeout(): Int = getInt("pref_ai_timeout", 10)
    fun getAiTemperature(): Double = getDouble("pref_ai_temperature", 0.1)
    fun getAiMaxTokens(): Int = getInt("pref_ai_max_tokens", 4096)
    fun getBatchSize(): Int = getInt("pref_batch_size", 20)
    fun getBatchWindowMs(): Long = getInt("pref_batch_window_ms", 100).toLong()
    fun isCacheEnabled(): Boolean = getBoolean("pref_cache_enabled", true)
    fun isTranslateToast(): Boolean = getBoolean("pref_translate_toast", true)
    fun getAppWhitelist(): Set<String> {
        val raw = getString("pref_app_whitelist", "")
        if (raw.isEmpty()) return emptySet()
        return raw.split(",").filter { it.isNotEmpty() }.toSet()
    }

    // ── 内部实现 ────────────────────────────────────────────────────────────

    private fun fk(key: String) = "$FLUTTER_KEY_PREFIX$key"

    private fun prefsForKey(key: String): SharedPreferences? {
        if (isCoreKey(key)) return corePrefs
        val index = shardForKey(fk(key))
        shardPrefs[index]?.let { return it }
        val m = module ?: return null
        return synchronized(this) {
            shardPrefs[index] ?: try {
                m.getRemotePreferences("$PREFS_SHARD_PREFIX$index").also { prefs ->
                    prefs.registerOnSharedPreferenceChangeListener(prefsListener)
                    shardPrefs[index] = prefs
                    m.log(Log.INFO, TAG, "remote prefs '$PREFS_SHARD_PREFIX$index' loaded")
                }
            } catch (e: Throwable) {
                m.log(Log.ERROR, TAG, "shard $index load failed: ${e.message}")
                null
            }
        }
    }

    private fun isCoreKey(key: String): Boolean = key in CORE_PREF_KEYS

    private fun shardForKey(key: String): Int =
        (key.hashCode() and Int.MAX_VALUE) % SHARD_COUNT

    private fun notifyListeners() {
        val ls = synchronized(this) { changeListeners.toList() }
        ls.forEach { runCatching { it() } }
    }

    private val CORE_PREF_KEYS = setOf(
        "pref_translation_enabled", "pref_ai_url", "pref_ai_api_key",
        "pref_ai_model", "pref_ai_prompt", "pref_ai_target_lang",
        "pref_ai_timeout", "pref_ai_temperature", "pref_ai_max_tokens",
        "pref_batch_size", "pref_batch_window_ms", "pref_cache_enabled",
        "pref_translate_toast", "pref_app_whitelist",
        "pref_theme_mode", "pref_theme_seed_color", "pref_blur_bars",
        "pref_debug_log", "pref_onboarding_completed"
    )
}
