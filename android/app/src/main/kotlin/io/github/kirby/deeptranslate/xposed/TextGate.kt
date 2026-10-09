package io.github.kirby.deeptranslate.xposed

/** Shared skip rules for every hook, so a translation is not sent back to the API. */
object TextGate {

    private val URL = Regex("^https?://\\S+$")

    fun shouldTranslate(text: String): Boolean {
        if (text.isBlank() || text.length < 2 || text.length > 4000) return false
        val trimmed = text.trim()
        if (trimmed.length < 2) return false
        if (URL.matches(trimmed)) return false
        if (trimmed.all { it.isDigit() || it.isWhitespace() }) return false
        if (TranslationCache.isKnownOutput(trimmed) || TranslationCache.isKnownOutput(text)) return false
        return LanguageDetector.needsTranslation(trimmed, ConfigManager.getTargetLang())
    }
}
