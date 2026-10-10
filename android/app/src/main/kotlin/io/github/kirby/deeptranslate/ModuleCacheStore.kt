package io.github.kirby.deeptranslate

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** The only translation database. It lives in this app. */
object ModuleCacheStore {
    private const val DB_NAME = "module_translation_cache.db"
    private const val DB_VERSION = 1
    private const val ROWS = "rows"
    private const val OUTPUTS = "outputs"
    private val lock = Any()
    private var helper: Helper? = null

    data class Row(
        val hash: String,
        val original: String,
        val translated: String,
        val lang: String,
        val time: Long,
    )

    fun get(context: Context, pkg: String, hash: String): String? {
        synchronized(lock) {
            return db(context).query(
                ROWS,
                arrayOf("translated"),
                "package = ? AND hash = ?",
                arrayOf(pkg, hash),
                null, null, null, "1",
            ).use { if (it.moveToFirst()) it.getString(0) else null }
        }
    }

    fun upsert(context: Context, pkg: String, rows: List<Row>) {
        if (rows.isEmpty()) return
        synchronized(lock) {
            val database = db(context)
            database.beginTransaction()
            try {
                for (row in rows) {
                    val cv = ContentValues().apply {
                        put("package", pkg)
                        put("hash", row.hash)
                        put("original", row.original)
                        put("translated", row.translated)
                        put("lang", row.lang)
                        put("time", row.time)
                    }
                    database.insertWithOnConflict(ROWS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                trim(database, pkg)
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
    }

    fun isOutput(context: Context, pkg: String, hash: String): Boolean {
        synchronized(lock) {
            return db(context).query(
                OUTPUTS,
                arrayOf("hash"),
                "package = ? AND hash = ?",
                arrayOf(pkg, hash),
                null, null, null, "1",
            ).use { it.moveToFirst() }
        }
    }

    fun markOutput(context: Context, pkg: String, hash: String) {
        synchronized(lock) {
            val cv = ContentValues().apply {
                put("package", pkg)
                put("hash", hash)
            }
            db(context).insertWithOnConflict(OUTPUTS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    fun list(context: Context, pkg: String, query: String): List<Map<String, Any>> {
        synchronized(lock) {
            val database = db(context)
            trim(database, pkg)
            val like = query.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            val args = mutableListOf(pkg)
            val where = StringBuilder("package = ?")
            if (like.isNotEmpty()) {
                where.append(" AND (original LIKE ? ESCAPE '\\' OR translated LIKE ? ESCAPE '\\')")
                args.add("%$like%")
                args.add("%$like%")
            }
            val argArray = args.toTypedArray()
            val total = database.rawQuery("SELECT COUNT(*) FROM $ROWS WHERE $where", argArray).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            val out = mutableListOf<Map<String, Any>>()
            database.query(
                ROWS,
                arrayOf("hash", "original", "translated", "lang", "time"),
                where.toString(),
                argArray,
                null,
                null,
                "time DESC",
                "500",
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    out.add(
                        mapOf(
                            "hash" to cursor.getString(0),
                            "original" to cursor.getString(1).take(180),
                            "translated" to cursor.getString(2).take(180),
                            "lang" to (cursor.getString(3) ?: ""),
                            "time" to cursor.getLong(4),
                            "total" to total,
                        )
                    )
                }
            }
            return out
        }
    }

    fun detail(context: Context, pkg: String, hash: String): Map<String, Any>? {
        synchronized(lock) {
            return db(context).query(
                ROWS,
                arrayOf("hash", "original", "translated", "lang", "time"),
                "package = ? AND hash = ?",
                arrayOf(pkg, hash),
                null, null, null, "1",
            ).use { cursor ->
                if (!cursor.moveToFirst()) null
                else mapOf(
                    "hash" to cursor.getString(0),
                    "original" to cursor.getString(1),
                    "translated" to cursor.getString(2),
                    "lang" to (cursor.getString(3) ?: ""),
                    "time" to cursor.getLong(4),
                )
            }
        }
    }

    fun delete(context: Context, pkg: String, hashes: Collection<String>) {
        if (hashes.isEmpty()) return
        synchronized(lock) {
            val marks = hashes.joinToString(",") { "?" }
            db(context).delete(ROWS, "package = ? AND hash IN ($marks)", arrayOf(pkg) + hashes.toTypedArray())
        }
    }

    fun clearPackage(context: Context, pkg: String) {
        synchronized(lock) {
            val database = db(context)
            database.delete(ROWS, "package = ?", arrayOf(pkg))
            database.delete(OUTPUTS, "package = ?", arrayOf(pkg))
        }
    }

    fun clearAll(context: Context) {
        synchronized(lock) {
            val database = db(context)
            database.delete(ROWS, null, null)
            database.delete(OUTPUTS, null, null)
        }
    }

    fun count(context: Context): Int = count(context, null)

    fun count(context: Context, pkg: String?): Int {
        synchronized(lock) {
            val sql = if (pkg == null) "SELECT COUNT(*) FROM $ROWS" else "SELECT COUNT(*) FROM $ROWS WHERE package = ?"
            val args = if (pkg == null) null else arrayOf(pkg)
            return db(context).rawQuery(sql, args).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        }
    }

    fun packages(context: Context): List<Pair<String, Int>> {
        synchronized(lock) {
            val out = mutableListOf<Pair<String, Int>>()
            db(context).rawQuery(
                "SELECT package, COUNT(*) FROM $ROWS GROUP BY package ORDER BY COUNT(*) DESC",
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) out.add(cursor.getString(0) to cursor.getInt(1))
            }
            return out
        }
    }

    private fun trim(database: SQLiteDatabase, pkg: String) {
        val limit = limit()
        database.execSQL(
            "DELETE FROM $ROWS WHERE package = ? AND hash NOT IN (" +
                "SELECT hash FROM $ROWS WHERE package = ? ORDER BY time DESC LIMIT $limit)",
            arrayOf(pkg, pkg),
        )
    }

    private fun limit(): Int {
        val raw = helper?.context?.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            ?.all?.get("flutter.pref_cache_limit")
        val n = when (raw) {
            is Int -> raw
            is Long -> raw.toInt()
            is String -> raw.toIntOrNull() ?: 10000
            else -> 10000
        }
        return n.coerceIn(100, 100_000)
    }

    private fun db(context: Context): SQLiteDatabase {
        val h = helper ?: Helper(context.applicationContext).also { helper = it }
        return h.writableDatabase
    }

    private class Helper(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $ROWS (
                    package TEXT NOT NULL,
                    hash TEXT NOT NULL,
                    original TEXT NOT NULL,
                    translated TEXT NOT NULL,
                    lang TEXT,
                    time INTEGER,
                    PRIMARY KEY(package, hash)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $OUTPUTS (
                    package TEXT NOT NULL,
                    hash TEXT NOT NULL,
                    PRIMARY KEY(package, hash)
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
