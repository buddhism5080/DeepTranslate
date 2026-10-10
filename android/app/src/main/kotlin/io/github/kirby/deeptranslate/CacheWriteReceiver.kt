package io.github.kirby.deeptranslate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

/** Same path as the error log: the target app does not have to see this app's provider. */
class CacheWriteReceiver : BroadcastReceiver() {
    private val packageNamePattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra("package") ?: return
        if (!packageNamePattern.matches(pkg)) return
        val expected = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            .getString("flutter.pref_broadcast_token", "") ?: ""
        if (!ModuleBroadcast.tokenMatches(intent, expected)) return
        if (android.os.Build.VERSION.SDK_INT >= 34 && sentFromPackage != pkg) return
        val op = intent.getStringExtra("op") ?: return
        val extra = Bundle().apply {
            putString("rows", intent.getStringExtra("rows"))
            putString("hash", intent.getStringExtra("hash"))
            val hashes = intent.getStringArrayListExtra("hashes")
            if (hashes != null) putStringArrayList("hashes", hashes)
        }
        val result = CacheOps.handle(context, pkg, op, extra) ?: return
        setResult(android.app.Activity.RESULT_OK, null, result)
    }
}
