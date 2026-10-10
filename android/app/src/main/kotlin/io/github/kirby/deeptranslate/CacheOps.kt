package io.github.kirby.deeptranslate

import android.content.Context
import android.os.Bundle
import org.json.JSONArray

/** Cache reads and writes. Callers must already have checked the token and the package. */
object CacheOps {
    private val hashPattern = Regex("^[0-9a-f]{64}$")

    fun handle(ctx: Context, caller: String, method: String, extra: Bundle): Bundle? {
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
}
