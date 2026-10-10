package io.github.kirby.deeptranslate

import android.content.Context
import android.content.Intent
import android.os.Build

/** Cross-app broadcasts must carry the module token and, on Android 14+, the sender identity. */
object ModuleBroadcast {
    const val EXTRA_TOKEN = "token"

    fun send(context: Context, intent: Intent, token: String) {
        if (token.isNotEmpty()) intent.putExtra(EXTRA_TOKEN, token)
        if (Build.VERSION.SDK_INT >= 34) {
            val options = android.app.BroadcastOptions.makeBasic()
            options.setShareIdentityEnabled(true)
            context.sendBroadcast(intent, null, options.toBundle())
        } else {
            context.sendBroadcast(intent)
        }
    }

    fun tokenMatches(intent: Intent?, expected: String): Boolean {
        if (expected.isEmpty()) return false
        return intent?.getStringExtra(EXTRA_TOKEN) == expected
    }
}
