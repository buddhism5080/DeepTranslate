package io.github.kirby.deeptranslate.xposed

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.security.MessageDigest
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * 翻译缓存。SQLite 持久化，key = SHA256(目标语言 + 原文)。
 * 另有一张 outputs 表，记下译文本身，避免译文再次送去翻译。
 */
object TranslationCache {

    private const val TAG = "DeepTranslate[Cache]"
    private const val DB_NAME = "deeptranslate_cache.db"
    private const val DB_VERSION = 2
    private const val TABLE = "translations"
    private const val OUTPUTS = "outputs"
    private const val COL_HASH = "text_hash"
    private const val COL_ORIGINAL = "original_text"
    private const val COL_TRANSLATED = "translated_text"
    private const val COL_SOURCE_LANG = "source_lang"
    private const val COL_PKG = "package_name"
    private const val COL_TIMESTAMP = "created_at"

    @Volatile private var dbHelper: DbHelper? = null
    @Volatile private var initialized = false

    private val memory = Collections.synchronizedMap(object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 400
    })
    private val knownOutputs = ConcurrentHashMap.newKeySet<String>()
    private val knownNotOutputs = ConcurrentHashMap.newKeySet<String>()

    @Synchronized
    fun init(context: Context) {
        if (initialized && dbHelper != null) return
        try {
            dbHelper = DbHelper(context.applicationContext)
            initialized = true
            Log.d(TAG, "cache initialized")
        } catch (e: Exception) {
            Log.e(TAG, "init failed: ${e.message}")
            initialized = true
        }
    }

    fun ensureFromApp(): Boolean {
        if (initialized && dbHelper != null) return true
        val ctx = try {
            val at = Class.forName("android.app.ActivityThread")
            at.getMethod("currentApplication").invoke(null) as? Context
        } catch (_: Throwable) {
            null
        } ?: return false
        init(ctx)
        return dbHelper != null
    }

    private fun ensureInit(): Boolean {
        if (!initialized || dbHelper == null) ensureFromApp()
        return dbHelper != null
    }

    fun get(original: String): String? {
        memory[scoped(original)]?.let { return it }
        if (!ensureInit()) return null
        return try {
            val hash = sha256(scoped(original))
            val db = dbHelper!!.readableDatabase
            val cursor = db.query(
                TABLE,
                arrayOf(COL_TRANSLATED),
                "$COL_HASH = ?",
                arrayOf(hash),
                null, null, null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val translated = it.getString(0)
                    memory[scoped(original)] = translated
                    translated
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "get failed: ${e.message}")
            null
        }
    }

    fun put(original: String, translated: String, sourceLang: String, pkg: String) {
        memory[scoped(original)] = translated
        if (translated != original) markOutput(translated)
        if (!ensureInit()) return
        try {
            val hash = sha256(scoped(original))
            val cv = ContentValues().apply {
                put(COL_HASH, hash)
                put(COL_ORIGINAL, original)
                put(COL_TRANSLATED, translated)
                put(COL_SOURCE_LANG, sourceLang)
                put(COL_PKG, pkg)
                put(COL_TIMESTAMP, System.currentTimeMillis())
            }
            val db = dbHelper!!.writableDatabase
            db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.w(TAG, "put failed: ${e.message}")
        }
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
        for (entry in entries) {
            memory[scoped(entry.original)] = entry.translated
            if (entry.translated != entry.original) markOutput(entry.translated)
        }
        if (!ensureInit()) return
        try {
            val db = dbHelper!!.writableDatabase
            db.beginTransaction()
            for (entry in entries) {
                val hash = sha256(scoped(entry.original))
                val cv = ContentValues().apply {
                    put(COL_HASH, hash)
                    put(COL_ORIGINAL, entry.original)
                    put(COL_TRANSLATED, entry.translated)
                    put(COL_SOURCE_LANG, entry.sourceLang)
                    put(COL_PKG, pkg)
                    put(COL_TIMESTAMP, System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        } catch (e: Exception) {
            Log.w(TAG, "putBatch failed: ${e.message}")
        }
    }

    /** 译文本身。命中后不再送去翻译，避免布局刷新打成环。 */
    fun isKnownOutput(text: String): Boolean {
        if (text.isEmpty()) return false
        val hash = sha256(scoped(text))
        if (knownOutputs.contains(hash)) return true
        if (knownNotOutputs.contains(hash)) return false
        if (!ensureInit()) return false
        return try {
            val db = dbHelper!!.readableDatabase
            val cursor = db.query(OUTPUTS, arrayOf(COL_HASH), "$COL_HASH = ?", arrayOf(hash), null, null, null)
            val hit = cursor.use { it.moveToFirst() }
            if (hit) knownOutputs.add(hash) else {
                if (knownNotOutputs.size > 2000) knownNotOutputs.clear()
                knownNotOutputs.add(hash)
            }
            hit
        } catch (_: Exception) {
            false
        }
    }

    fun markOutput(text: String) {
        if (text.isEmpty()) return
        val hash = sha256(scoped(text))
        knownNotOutputs.remove(hash)
        if (knownOutputs.size > 4000) knownOutputs.clear()
        knownOutputs.add(hash)
        if (!ensureInit()) return
        try {
            val cv = ContentValues().apply { put(COL_HASH, hash) }
            dbHelper!!.writableDatabase.insertWithOnConflict(OUTPUTS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        } catch (e: Exception) {
            Log.w(TAG, "markOutput failed: ${e.message}")
        }
    }

    fun clear() {
        memory.clear()
        knownOutputs.clear()
        knownNotOutputs.clear()
        if (!ensureInit()) return
        try {
            val db = dbHelper!!.writableDatabase
            db.delete(TABLE, null, null)
            db.delete(OUTPUTS, null, null)
            Log.d(TAG, "cache cleared")
        } catch (e: Exception) {
            Log.w(TAG, "clear failed: ${e.message}")
        }
    }

    fun count(): Int {
        if (!ensureInit()) return 0
        return try {
            val db = dbHelper!!.readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE", null)
            cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
        } catch (e: Exception) {
            0
        }
    }

    private fun scoped(original: String) = ConfigManager.getTargetLang() + "\u0000" + original

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

    private class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE (
                    $COL_HASH TEXT PRIMARY KEY,
                    $COL_ORIGINAL TEXT NOT NULL,
                    $COL_TRANSLATED TEXT NOT NULL,
                    $COL_SOURCE_LANG TEXT,
                    $COL_PKG TEXT,
                    $COL_TIMESTAMP INTEGER
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_pkg ON $TABLE($COL_PKG)")
            db.execSQL("CREATE TABLE IF NOT EXISTS $OUTPUTS ($COL_HASH TEXT PRIMARY KEY)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                db.execSQL("CREATE TABLE IF NOT EXISTS $OUTPUTS ($COL_HASH TEXT PRIMARY KEY)")
            }
        }
    }
}
