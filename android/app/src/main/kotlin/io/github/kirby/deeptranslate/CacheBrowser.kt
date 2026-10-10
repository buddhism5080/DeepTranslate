package io.github.kirby.deeptranslate

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * The translation database lives in the target app. Read it by copying the file
 * with root, and delete rows either in the live process or in that file.
 */
object CacheBrowser {
    private val hashPattern = Regex("^[0-9a-f]{64}$")

    fun list(context: Context, pkg: String, query: String): List<Map<String, Any>>? {
        val file = copyDb(context, pkg) ?: return null
        return read(file) { db ->
            val like = query.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            val args = if (like.isEmpty()) null else arrayOf("%$like%", "%$like%")
            val where = if (like.isEmpty()) null else "original_text LIKE ? ESCAPE '\\' OR translated_text LIKE ? ESCAPE '\\'"
            val total = db.rawQuery(
                "SELECT COUNT(*) FROM translations" + (if (where == null) "" else " WHERE $where"),
                args,
            ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            val rows = mutableListOf<Map<String, Any>>()
            db.query(
                "translations",
                arrayOf("text_hash", "original_text", "translated_text", "source_lang", "created_at"),
                where,
                args,
                null,
                null,
                "created_at DESC",
                "200",
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    rows.add(
                        mapOf(
                            "hash" to cursor.getString(0),
                            "original" to cursor.getString(1).take(180),
                            "translated" to cursor.getString(2).take(180),
                            "lang" to (cursor.getString(3) ?: ""),
                            "time" to cursor.getLong(4),
                        )
                    )
                }
            }
            rows.map { it + ("total" to total) }
        }
    }

    fun detail(context: Context, pkg: String, hash: String): Map<String, Any>? {
        if (!hashPattern.matches(hash)) return null
        val file = copyDb(context, pkg) ?: return null
        return read(file) { db ->
            db.query(
                "translations",
                arrayOf("text_hash", "original_text", "translated_text", "source_lang", "created_at"),
                "text_hash = ?",
                arrayOf(hash),
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

    fun delete(context: Context, pkg: String, hashes: List<String>): Boolean {
        val safe = hashes.filter { hashPattern.matches(it) }
        if (safe.isEmpty()) return false
        val intent = Intent("io.github.kirby.deeptranslate.DELETE_CACHE").apply {
            setPackage(pkg)
            putStringArrayListExtra("hashes", ArrayList(safe))
        }
        context.sendBroadcast(intent, "io.github.kirby.deeptranslate.permission.CACHE")
        if (processRunning(pkg)) return true
        val file = copyDb(context, pkg) ?: return false
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            val marks = safe.joinToString(",") { "?" }
            db.delete("translations", "text_hash IN ($marks)", safe.toTypedArray())
        } finally {
            db.close()
        }
        val src = "/data/data/$pkg/databases/deeptranslate_cache.db"
        val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "cp -f ${file.absolutePath} $src && chmod 600 $src"))
        return proc.waitFor() == 0
    }

    private fun processRunning(pkg: String): Boolean {
        val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "pidof $pkg"))
        val out = proc.inputStream.bufferedReader().use { it.readText() }.trim()
        proc.waitFor()
        return out.isNotEmpty()
    }

    private fun copyDb(context: Context, pkg: String): File? {
        val dest = File(context.cacheDir, "peek-$pkg.db")
        val src = "/data/data/$pkg/databases/deeptranslate_cache.db"
        val proc = Runtime.getRuntime().exec(
            arrayOf("su", "-c", "cp -f $src ${dest.absolutePath} && chmod 644 ${dest.absolutePath}"),
        )
        if (proc.waitFor() != 0 || !dest.exists() || dest.length() == 0L) return null
        return dest
    }

    private fun <T> read(file: File, block: (SQLiteDatabase) -> T): T {
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        return try {
            block(db)
        } finally {
            db.close()
        }
    }
}
