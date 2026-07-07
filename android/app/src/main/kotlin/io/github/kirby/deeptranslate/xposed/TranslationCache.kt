package io.github.kirby.deeptranslate.xposed

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.security.MessageDigest

/**
 * 翻译缓存。SQLite 持久化，key = SHA256(原文)。
 * 翻译过的文本下次直接命中缓存，零 API 调用、零延迟。
 */
object TranslationCache {

    private const val TAG = "DeepTranslate[Cache]"
    private const val DB_NAME = "deeptranslate_cache.db"
    private const val DB_VERSION = 1
    private const val TABLE = "translations"
    private const val COL_HASH = "text_hash"
    private const val COL_ORIGINAL = "original_text"
    private const val COL_TRANSLATED = "translated_text"
    private const val COL_SOURCE_LANG = "source_lang"
    private const val COL_PKG = "package_name"
    private const val COL_TIMESTAMP = "created_at"

    @Volatile private var dbHelper: DbHelper? = null
    @Volatile private var initialized = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        try {
            dbHelper = DbHelper(context.applicationContext)
            initialized = true
            Log.d(TAG, "cache initialized")
        } catch (e: Exception) {
            Log.e(TAG, "init failed: ${e.message}")
            initialized = true
        }
    }

    private fun ensureInit(): Boolean {
        if (!initialized) {
            Log.w(TAG, "cache not initialized, skipping")
            return false
        }
        return dbHelper != null
    }

    /** 查询缓存，命中返回译文，未命中返回 null。 */
    fun get(original: String): String? {
        if (!ensureInit()) return null
        return try {
            val hash = sha256(original)
            val db = dbHelper!!.readableDatabase
            val cursor = db.query(
                TABLE,
                arrayOf(COL_TRANSLATED),
                "$COL_HASH = ?",
                arrayOf(hash),
                null, null, null
            )
            cursor.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "get failed: ${e.message}")
            null
        }
    }

    /** 写入缓存。 */
    fun put(original: String, translated: String, sourceLang: String, pkg: String) {
        if (!ensureInit()) return
        try {
            val hash = sha256(original)
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

    /** 批量查询缓存，返回 Map<原文, 译文>（仅命中的）。 */
    fun getBatch(originals: List<String>): Map<String, String> {
        if (!ensureInit() || originals.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        try {
            val db = dbHelper!!.readableDatabase
            for (text in originals) {
                val cached = get(text)
                if (cached != null) result[text] = cached
            }
        } catch (e: Exception) {
            Log.w(TAG, "getBatch failed: ${e.message}")
        }
        return result
    }

    /** 批量写入缓存。 */
    fun putBatch(entries: List<CacheEntry>, pkg: String) {
        if (!ensureInit() || entries.isEmpty()) return
        try {
            val db = dbHelper!!.writableDatabase
            db.beginTransaction()
            for (entry in entries) {
                val hash = sha256(entry.original)
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

    /** 清空全部缓存。 */
    fun clear() {
        if (!ensureInit()) return
        try {
            dbHelper!!.writableDatabase.delete(TABLE, null, null)
            Log.d(TAG, "cache cleared")
        } catch (e: Exception) {
            Log.w(TAG, "clear failed: ${e.message}")
        }
    }

    /** 缓存条目数。 */
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
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE")
            onCreate(db)
        }
    }
}
