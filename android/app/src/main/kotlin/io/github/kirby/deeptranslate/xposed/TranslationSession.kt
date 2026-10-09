package io.github.kirby.deeptranslate.xposed

import io.github.libxposed.api.XposedModule

/** One batcher per hooked process, shared by TextView / layout / WebView hooks. */
object TranslationSession {

    @Volatile private var batcher: TextBatcher? = null

    fun batcher(module: XposedModule, packageName: String): TextBatcher {
        batcher?.let { return it }
        return synchronized(this) {
            batcher ?: TextBatcher(module, packageName).also { batcher = it }
        }
    }
}
