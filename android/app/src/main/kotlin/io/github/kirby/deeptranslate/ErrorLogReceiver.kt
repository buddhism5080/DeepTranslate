package io.github.kirby.deeptranslate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Hooked apps report API failures here. The settings page reads the same file. */
class ErrorLogReceiver : BroadcastReceiver() {
    private val packageNamePattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra("package") ?: return
        if (!packageNamePattern.matches(pkg)) return
        if (android.os.Build.VERSION.SDK_INT >= 34 && sentFromPackage != pkg) return
        val kind = intent.getStringExtra("kind") ?: return
        if (kind !in setOf("network", "http", "parse")) return
        ErrorLogStore.append(
            context,
            pkg,
            kind,
            intent.getIntExtra("status", 0),
            intent.getStringExtra("message") ?: "",
            intent.getStringExtra("request") ?: "",
            intent.getStringExtra("response") ?: "",
        )
    }
}
