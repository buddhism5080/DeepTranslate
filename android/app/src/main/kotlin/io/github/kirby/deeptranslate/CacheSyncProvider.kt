package io.github.kirby.deeptranslate

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import org.json.JSONArray

/** Hooked apps read and write the module cache through this. Nothing is stored in the target app. */
class CacheSyncProvider : ContentProvider() {
    private val packageNamePattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val hashPattern = Regex("^[0-9a-f]{64}$")

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        val extra = extras ?: return null
        val caller = callingPackage ?: return null
        if (!packageNamePattern.matches(caller)) return null
        if (extra.getString("package") != caller) return null
        val expected = ctx.getSharedPreferences("FlutterSharedPreferences", android.content.Context.MODE_PRIVATE)
            .getString("flutter.pref_broadcast_token", "") ?: ""
        val token = extra.getString("token") ?: return null
        if (expected.isEmpty() || token != expected) return null
        return when (method) {
            "get" -> {
                val hash = extra.getString("hash") ?: return null
                if (!hashPattern.matches(hash)) return null
                val translated = ModuleCacheStore.get(ctx, caller, hash)
                Bundle().apply {
                    if (translated == null) putBoolean("miss", true) else putString("translated", translated)
                }
            }
            "put" -> {
                ModuleCacheStore.upsert(ctx, caller, rows(extra.getString("rows")))
                Bundle().apply { putBoolean("ok", true) }
            }
            "known" -> {
                val hash = extra.getString("hash") ?: return null
                if (!hashPattern.matches(hash)) return null
                Bundle().apply { putBoolean("hit", ModuleCacheStore.isOutput(ctx, caller, hash)) }
            }
            "mark" -> {
                val hash = extra.getString("hash") ?: return null
                if (!hashPattern.matches(hash)) return null
                ModuleCacheStore.markOutput(ctx, caller, hash)
                Bundle().apply { putBoolean("ok", true) }
            }
            "delete" -> {
                val hashes = extra.getStringArrayList("hashes")?.filter { hashPattern.matches(it) } ?: return null
                ModuleCacheStore.delete(ctx, caller, hashes)
                Bundle().apply { putBoolean("ok", true) }
            }
            "clear" -> {
                ModuleCacheStore.clearPackage(ctx, caller)
                Bundle().apply { putBoolean("ok", true) }
            }
            else -> null
        }
    }

    private fun rows(raw: String?): List<ModuleCacheStore.Row> {
        if (raw.isNullOrEmpty()) return emptyList()
        val array = try {
            JSONArray(raw)
        } catch (_: Exception) {
            return emptyList()
        }
        val out = mutableListOf<ModuleCacheStore.Row>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val hash = obj.optString("hash")
            if (!hashPattern.matches(hash)) continue
            val original = obj.optString("original")
            val translated = obj.optString("translated")
            if (original.isEmpty() || translated.isEmpty()) continue
            out.add(
                ModuleCacheStore.Row(
                    hash = hash,
                    original = original,
                    translated = translated,
                    lang = obj.optString("lang"),
                    time = obj.optLong("time"),
                )
            )
        }
        return out
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
