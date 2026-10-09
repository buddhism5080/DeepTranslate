package io.github.kirby.deeptranslate.xposed

/**
 * Splits content so the pieces concatenate back to the original.
 * A chunk stops at [maxChars] or [maxParagraphs], whichever comes first.
 * Cuts prefer a paragraph, then a sentence, then whitespace or punctuation.
 */
object TextChunks {
    fun split(text: String, maxChars: Int, maxParagraphs: Int): List<String> {
        val charLimit = maxChars.coerceAtLeast(200)
        val paraLimit = maxParagraphs.coerceAtLeast(1)
        if (text.isEmpty()) return listOf(text)
        val pieces = paragraphPieces(text).flatMap { splitOversized(it, charLimit) }
        if (pieces.size <= paraLimit && text.length <= charLimit) return listOf(text)

        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        var paras = 0
        for (piece in pieces) {
            val overflow = current.isNotEmpty() &&
                (current.length + piece.length > charLimit || paras + 1 > paraLimit)
            if (overflow) {
                chunks.add(current.toString())
                current.clear()
                paras = 0
            }
            current.append(piece)
            paras++
        }
        if (current.isNotEmpty()) chunks.add(current.toString())
        return chunks.ifEmpty { listOf(text) }
    }

    private fun paragraphPieces(text: String): List<String> {
        val blank = splitKeeping(text, Regex("\\n[ \\t]*\\n"))
        if (blank.size > 1) return blank
        if (!text.contains('\n')) return listOf(text)
        return splitKeepingChar(text, '\n')
    }

    private fun splitKeeping(text: String, pattern: Regex): List<String> {
        val out = mutableListOf<String>()
        var start = 0
        for (match in pattern.findAll(text)) {
            out.add(text.substring(start, match.range.last + 1))
            start = match.range.last + 1
        }
        if (start < text.length) out.add(text.substring(start))
        return out.ifEmpty { listOf(text) }
    }

    private fun splitKeepingChar(text: String, ch: Char): List<String> {
        val out = mutableListOf<String>()
        var start = 0
        for (i in text.indices) {
            if (text[i] == ch) {
                out.add(text.substring(start, i + 1))
                start = i + 1
            }
        }
        if (start < text.length) out.add(text.substring(start))
        return out.ifEmpty { listOf(text) }
    }

    private fun splitOversized(piece: String, maxChars: Int): List<String> {
        if (piece.length <= maxChars) return listOf(piece)
        return pack(splitSentences(piece), maxChars)
    }

    private fun splitSentences(text: String): List<String> {
        val out = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val boundary = when (c) {
                '。', '！', '？', '!', '?' -> true
                '.' -> {
                    val next = if (i + 1 < text.length) text[i + 1] else ' '
                    val prevDigit = i > 0 && text[i - 1].isDigit()
                    !prevDigit && (next == ' ' || next == '\n')
                }
                else -> false
            }
            if (boundary) {
                out.add(text.substring(start, i + 1))
                start = i + 1
            }
            i++
        }
        if (start < text.length) out.add(text.substring(start))
        return out.ifEmpty { listOf(text) }
    }

    private fun pack(parts: List<String>, maxChars: Int): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        for (part in parts) {
            if (part.length > maxChars) {
                if (current.isNotEmpty()) {
                    out.add(current.toString())
                    current.clear()
                }
                out.addAll(hardSplit(part, maxChars))
                continue
            }
            if (current.isNotEmpty() && current.length + part.length > maxChars) {
                out.add(current.toString())
                current.clear()
            }
            current.append(part)
        }
        if (current.isNotEmpty()) out.add(current.toString())
        return out.ifEmpty { listOf(parts.joinToString("")) }
    }

    private fun hardSplit(text: String, maxChars: Int): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            var end = (i + maxChars).coerceAtMost(text.length)
            if (end < text.length) {
                val windowStart = (end - maxChars / 5).coerceAtLeast(i + 1)
                val window = text.substring(windowStart, end)
                val rel = window.lastIndexOfAny(charArrayOf(' ', '\n', '，', ',', '、', '；', ';', '。'))
                if (rel >= 0) end = windowStart + rel + 1
            }
            if (end <= i) end = (i + maxChars).coerceAtMost(text.length)
            out.add(text.substring(i, end))
            i = end
        }
        return out
    }
}
