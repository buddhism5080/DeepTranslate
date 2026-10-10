package io.github.kirby.deeptranslate

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/** Hooked apps read and write the module cache through this. Nothing is stored in the target app. */
class CacheSyncProvider : ContentProvider() {
    private val packageNamePattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

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
        return CacheOps.handle(ctx, caller, method, extra)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
