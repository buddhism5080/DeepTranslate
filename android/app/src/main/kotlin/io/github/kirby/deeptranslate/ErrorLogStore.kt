package io.github.kirby.deeptranslate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Error rows live in this app, grouped by the package that made the call. */
object ErrorLogStore {
    private const val MAX_ROWS = 200
    private const val MAX_TEXT = 6000
    private val lock = Any()

    fun append(
        context: Context,
        pkg: String,
        kind: String,
        status: Int,
        message: String,
        request: String,
        response: String,
    ) {
        synchronized(lock) {
            val rows = readArray(context)
            val row = JSONObject()
            row.put("id", UUID.randomUUID().toString())
            row.put("package", pkg)
            row.put("time", System.currentTimeMillis())
            row.put("kind", kind)
            row.put("status", status)
            row.put("message", clip(message))
            row.put("request", clip(request))
            row.put("response", clip(response))
            rows.put(row)
            while (rows.length() > MAX_ROWS) rows.remove(0)
            write(context, rows)
        }
    }

    fun grouped(context: Context): List<Pair<String, List<Map<String, Any>>>> {
        synchronized(lock) {
            val rows = readArray(context)
            val order = linkedMapOf<String, MutableList<Map<String, Any>>>()
            for (i in rows.length() - 1 downTo 0) {
                val row = rows.optJSONObject(i) ?: continue
                val pkg = row.optString("package")
                if (pkg.isEmpty()) continue
                val list = order.getOrPut(pkg) { mutableListOf() }
                list.add(
                    mapOf(
                        "id" to row.optString("id"),
                        "time" to row.optLong("time"),
                        "kind" to row.optString("kind"),
                        "status" to row.optInt("status"),
                        "message" to row.optString("message"),
                        "request" to row.optString("request"),
                        "response" to row.optString("response"),
                    )
                )
            }
            return order.map { (pkg, entries) -> pkg to entries }
        }
    }

    fun delete(context: Context, ids: Set<String>) {
        if (ids.isEmpty()) return
        synchronized(lock) {
            val rows = readArray(context)
            val kept = JSONArray()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                if (row.optString("id") !in ids) kept.put(row)
            }
            write(context, kept)
        }
    }

    fun clear(context: Context, pkg: String?) {
        synchronized(lock) {
            if (pkg.isNullOrEmpty()) {
                write(context, JSONArray())
                return
            }
            val rows = readArray(context)
            val kept = JSONArray()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                if (row.optString("package") != pkg) kept.put(row)
            }
            write(context, kept)
        }
    }

    private fun readArray(context: Context): JSONArray {
        val file = file(context)
        if (!file.exists()) return JSONArray()
        return try {
            JSONArray(file.readText())
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun write(context: Context, rows: JSONArray) {
        file(context).writeText(rows.toString())
    }

    private fun file(context: Context) = File(context.filesDir, "error-logs.json")

    private fun clip(text: String): String =
        if (text.length <= MAX_TEXT) text else text.take(MAX_TEXT) + "…"
}
