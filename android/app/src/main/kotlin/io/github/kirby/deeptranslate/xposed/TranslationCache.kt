package io.github.kirby.deeptranslate.xposed

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * 翻译缓存只存在模块自己的数据库里。
 * 目标进程不落盘，只在内存里记住这次运行查过的结果。
 */
object TranslationCache {

    private const val TAG = "DeepTranslate[Cache]"
    private const val LEGACY_DB = "deeptranslate_cache.db"
    private val CACHE_URI: Uri = Uri.parse("content://io.github.kirby.deeptranslate.cache")

    @Volatile private var appContext: Context? = null
    @Volatile private var initialized = false

    private val memory = Collections.synchronizedMap(object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 400
    })
    private val knownOutputs = ConcurrentHashMap.newKeySet<String>()
    private val knownNotOutputs = ConcurrentHashMap.newKeySet<String>()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true
        Thread({ migrateLegacy(appContext!!) }, "dt-cache-migrate").start()
    }

    fun ensureFromApp(): Boolean {
        if (initialized && appContext != null) return true
        val ctx = try {
            val at = Class.forName("android.app.ActivityThread")
            at.getMethod("currentApplication").invoke(null) as? Context
        } catch (_: Throwable) {
            null
        } ?: return false
        init(ctx)
        return appContext != null
    }

    fun get(original: String, lang: String = ConfigManager.getTargetLang()): String? {
        memory[scoped(original, lang)]?.let { return it }
        val translated = call("get", Bundle().apply { putString("hash", sha256(scoped(original, lang))) })
            ?.getString("translated") ?: return null
        memory[scoped(original, lang)] = translated
        return translated
    }

    fun put(original: String, translated: String, sourceLang: String, pkg: String, lang: String = ConfigManager.getTargetLang()) {
        memory[scoped(original, lang)] = translated
        if (translated != original) markOutput(translated, lang)
        sendRows(
            listOf(row(original, translated, sourceLang, lang)),
        )
    }

    fun getBatch(originals: List<String>): Map<String, String> {
        if (originals.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        for (text in originals) {
            val cached = get(text)
            if (cached != null) result[text] = cached
        }
        return result
    }

    fun putBatch(entries: List<CacheEntry>, pkg: String) {
        if (entries.isEmpty()) return
        val lang = ConfigManager.getTargetLang()
        val rows = entries.map { entry ->
            memory[scoped(entry.original, lang)] = entry.translated
            if (entry.translated != entry.original) markOutput(entry.translated, lang)
            row(entry.original, entry.translated, entry.sourceLang, lang)
        }
        sendRows(rows)
    }

    fun deleteHashes(hashes: Collection<String>) {
        val safe = hashes.filter { it.length == 64 && it.all { ch -> ch in '0'..'9' || ch in 'a'..'f' } }
        memory.clear()
        if (safe.isEmpty()) return
        call("delete", Bundle().apply { putStringArrayList("hashes", ArrayList(safe)) })
    }

    /** 译文本身。命中后不再送去翻译，避免布局刷新打成环。 */
    fun isKnownOutput(text: String, lang: String = ConfigManager.getTargetLang()): Boolean {
        if (text.isEmpty()) return false
        val hash = sha256(scoped(text, lang))
        if (knownOutputs.contains(hash)) return true
        if (knownNotOutputs.contains(hash)) return false
        val bundle = call("known", Bundle().apply { putString("hash", hash) }) ?: return false
        val hit = bundle.getBoolean("hit", false)
        if (hit) knownOutputs.add(hash) else {
            if (knownNotOutputs.size > 2000) knownNotOutputs.clear()
            knownNotOutputs.add(hash)
        }
        return hit
    }

    fun markOutput(text: String, lang: String = ConfigManager.getTargetLang()) {
        if (text.isEmpty()) return
        val hash = sha256(scoped(text, lang))
        knownNotOutputs.remove(hash)
        if (knownOutputs.size > 4000) knownOutputs.clear()
        knownOutputs.add(hash)
        call("mark", Bundle().apply { putString("hash", hash) })
    }

    fun clear() {
        memory.clear()
        knownOutputs.clear()
        knownNotOutputs.clear()
        call("clear", Bundle())
    }

    fun count(): Int = 0

    private fun row(original: String, translated: String, sourceLang: String, lang: String): JSONObject {
        return JSONObject().apply {
            put("hash", sha256(scoped(original, lang)))
            put("original", original)
            put("translated", translated)
            put("lang", sourceLang)
            put("time", System.currentTimeMillis())
        }
    }

    private fun sendRows(rows: List<JSONObject>): Boolean {
        if (rows.isEmpty()) return true
        var batch = JSONArray()
        var size = 2
        var ok = true
        fun flush() {
            if (batch.length() == 0) return
            val sent = call("put", Bundle().apply { putString("rows", batch.toString()) })?.getBoolean("ok", false) == true
            if (!sent) {
                ok = false
                Log.w(TAG, "put failed")
            }
            batch = JSONArray()
            size = 2
        }
        for (row in rows) {
            val encoded = row.toString()
            if (batch.length() > 0 && size + encoded.length > 200_000) flush()
            batch.put(row)
            size += encoded.length
        }
        flush()
        return ok
    }

    private fun call(method: String, extras: Bundle): Bundle? {
        val ctx = appContext ?: if (ensureFromApp()) appContext else null
        if (ctx == null) return null
        val token = ConfigManager.getBroadcastToken()
        if (token.isEmpty()) return null
        extras.putString("token", token)
        extras.putString("package", ctx.packageName)
        return try {
            ctx.contentResolver.call(CACHE_URI, method, null, extras)
        } catch (e: Exception) {
            Log.w(TAG, "$method failed: ${e.message}")
            null
        }
    }

    /** 旧版本写在目标应用里的库，搬进模块后删掉。 */
    private fun migrateLegacy(context: Context) {
        val file = context.getDatabasePath(LEGACY_DB)
        if (!file.exists()) return
        try {
            val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            val rows = mutableListOf<JSONObject>()
            db.rawQuery(
                "SELECT text_hash, original_text, translated_text, source_lang, created_at FROM translations",
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val hash = cursor.getString(0) ?: continue
                    if (hash.length != 64) continue
                    rows.add(JSONObject().apply {
                        put("hash", hash)
                        put("original", cursor.getString(1) ?: "")
                        put("translated", cursor.getString(2) ?: "")
                        put("lang", cursor.getString(3) ?: "")
                        put("time", cursor.getLong(4))
                    })
                }
            }
            val outputs = mutableListOf<String>()
            try {
                db.rawQuery("SELECT text_hash FROM outputs", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val hash = cursor.getString(0) ?: continue
                        if (hash.length == 64) outputs.add(hash)
                    }
                }
            } catch (_: Exception) {
            }
            db.close()
            val moved = sendRows(rows.filter { it.optString("original").isNotEmpty() && it.optString("translated").isNotEmpty() })
            var marked = true
            for (hash in outputs) {
                if (call("mark", Bundle().apply { putString("hash", hash) }) == null) marked = false
            }
            if (moved && marked) {
                context.deleteDatabase(LEGACY_DB)
                Log.d(TAG, "moved ${rows.size} legacy rows into the module")
            }
        } catch (e: Exception) {
            Log.w(TAG, "legacy migrate failed: ${e.message}")
        }
    }

    private fun scoped(original: String, lang: String = ConfigManager.getTargetLang()) =
        lang + "\u0000" + original

    private fun sha256(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    data class CacheEntry(
        val original: String,
        val translated: String,
        val sourceLang: String
    )
}
