package io.github.kirby.deeptranslate.xposed

/** Bilingual text is built at display time. The cache keeps the translation only. */
object DisplayText {
    fun present(original: String, translated: String, kind: TextKind): String {
        if (!ConfigManager.isBilingual()) return translated
        if (translated.isEmpty() || translated == original) return translated
        return if (kind == TextKind.UI) {
            if (original.length <= 40 && translated.length <= 40) "$translated ($original)"
            else "$translated\n$original"
        } else {
            "$translated\n\n$original"
        }
    }

    fun remember(text: String) {
        if (text.isNotEmpty()) TranslationCache.markOutput(text)
    }
}
