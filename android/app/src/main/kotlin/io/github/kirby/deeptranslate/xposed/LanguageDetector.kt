package io.github.kirby.deeptranslate.xposed

/**
 * 轻量级语言检测。基于 Unicode 字符范围快速判断，
 * 仅识别「是否需要翻译」——如果已经是目标语言则跳过。
 *
 * 目标语言默认中文，所以只需检测文本是否包含大量中文字符。
 * 若中文比例 > 30%，认为已经是中文，不需要翻译。
 */
object LanguageDetector {

    /**
     * 判断文本是否需要翻译（即不是目标语言）。
     * @param text 待检测文本
     * @param targetLang 目标语言（默认"中文"）
     * @return true 表示需要翻译，false 表示已是目标语言或无需翻译
     */
    fun needsTranslation(text: String, targetLang: String = "中文"): Boolean {
        if (text.isBlank()) return false
        val trimmed = text.trim()
        if (trimmed.length < 2) return false  // 单字符不翻译

        // 纯数字、纯符号不翻译
        if (trimmed.all { it.isDigit() || isPunctuation(it) || it.isWhitespace() }) {
            return false
        }

        when (targetLang) {
            "中文", "Chinese", "简体中文" -> {
                val cjkCount = trimmed.count { isCJK(it) }
                val cjkRatio = cjkCount.toDouble() / trimmed.length
                // 如果超过 30% 是 CJK 字符，认为已经是中文
                if (cjkRatio > 0.3) return false
                // 必须有足够的拉丁/其他字母才翻译
                val letterCount = trimmed.count { it.isLetter() }
                return letterCount >= 2
            }
            else -> {
                // 其他目标语言：只要文本非空且有字母就翻译
                return trimmed.any { it.isLetter() }
            }
        }
    }

    /** 是否为 CJK 统一表意文字（中日韩汉字）。 */
    private fun isCJK(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x4E00..0x9FFF) ||   // CJK Unified Ideographs
               (code in 0x3400..0x4DBF) ||   // CJK Extension A
               (code in 0x20000..0x2A6DF) || // CJK Extension B
               (code in 0x3040..0x309F) ||   // Hiragana
               (code in 0x30A0..0x30FF)      // Katakana
    }

    private fun isPunctuation(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x21..0x2F) ||  // ASCII 符号
               (code in 0x3A..0x40) ||
               (code in 0x5B..0x60) ||
               (code in 0x7B..0x7E) ||
               (code in 0x3000..0x303F)   // CJK 标点
    }

    /**
     * 粗略判断文本是否为纯英文（拉丁字母为主）。
     * 用于决定是否走快速翻译路径。
     */
    fun isMostlyLatin(text: String): Boolean {
        if (text.isBlank()) return false
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return false
        return letters.all { it.code < 0x4E00 }
    }
}
