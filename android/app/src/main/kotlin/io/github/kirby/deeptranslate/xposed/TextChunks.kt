package io.github.kirby.deeptranslate.xposed

/**
 * Splits content so the pieces concatenate back to the original.
 * Cuts prefer a fenced block, paragraph, list item, quote, sentence, then a
 * space. URLs and closed code fences stay intact unless one of them is longer
 * than the limit by itself.
 */
object TextChunks {

    data class Piece(val text: String, val role: String)

    private val URL = Regex("https?://[^\\s<>\\]\"')]+")
    private val LIST = Regex("^(\\s*)([-*+]|\\d+[.)])\\s+\\S")
    private val QUOTE = Regex("^\\s*>")
    private val HEAD = Regex("^#{1,6}\\s+\\S")
    private val ABBREV = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "st", "vs", "etc",
        "eg", "ie", "am", "pm", "us", "uk", "no"
    )

    fun split(text: String, maxChars: Int, maxParagraphs: Int): List<String> =
        splitDetailed(text, maxChars, maxParagraphs).map { it.text }

    fun splitDetailed(text: String, maxChars: Int, maxParagraphs: Int): List<Piece> {
        val charLimit = maxChars.coerceAtLeast(200)
        val paraLimit = maxParagraphs.coerceAtLeast(1)
        if (text.isEmpty()) return listOf(Piece(text, "post"))
        val pieces = structural(text, charLimit).filter { it.text.isNotEmpty() }
        if (pieces.isEmpty() || pieces.joinToString("") { it.text } != text) {
            return listOf(Piece(text, "post"))
        }
        if (pieces.size == 1) return pieces

        val packed = mutableListOf<Piece>()
        val current = StringBuilder()
        var role = "post"
        var mixed = false
        var paras = 0
        fun flush() {
            if (current.isEmpty()) return
            packed.add(Piece(current.toString(), if (mixed) "post" else role))
            current.clear()
            role = "post"
            mixed = false
            paras = 0
        }
        for (piece in pieces) {
            val overflow = current.isNotEmpty() &&
                (current.length + piece.text.length > charLimit || paras + 1 > paraLimit)
            if (overflow) flush()
            if (current.isEmpty()) role = piece.role
            else if (piece.role != role) mixed = true
            current.append(piece.text)
            paras++
        }
        flush()
        if (packed.joinToString("") { it.text } != text) return listOf(Piece(text, "post"))
        return packed
    }

    fun contextHead(text: String, limit: Int = 160): String {
        val trimmed = text.trimStart()
        if (trimmed.length <= limit) return trimmed
        var end = limit
        val windowStart = (limit - 40).coerceAtLeast(0)
        val rel = trimmed.substring(windowStart, limit).lastIndexOfAny(charArrayOf(' ', '\n', '。', '.', '，', ','))
        if (rel >= 0) end = windowStart + rel + 1
        return trimmed.substring(0, end.coerceAtMost(trimmed.length))
    }

    fun contextTail(text: String, limit: Int = 160): String {
        val trimmed = text.trimEnd()
        if (trimmed.length <= limit) return trimmed
        var start = trimmed.length - limit
        val windowEnd = (start + 40).coerceAtMost(trimmed.length)
        val rel = trimmed.substring(start, windowEnd).indexOfAny(charArrayOf(' ', '\n', '。', '.', '，', ','))
        if (rel >= 0) start += rel + 1
        return trimmed.substring(start.coerceIn(0, trimmed.length))
    }

    private fun structural(text: String, maxChars: Int): List<Piece> {
        val out = mutableListOf<Piece>()
        var i = 0
        while (i < text.length) {
            val fenceEnd = if (atLineStart(text, i)) fenceBlock(text, i) else null
            if (fenceEnd != null) {
                out += explode(Piece(text.substring(i, fenceEnd), "code"), maxChars)
                i = fenceEnd
                continue
            }
            val next = nextFenceStart(text, i + 1)
            out += splitProse(text.substring(i, next), maxChars)
            i = next
        }
        return out
    }

    private fun splitProse(text: String, maxChars: Int): List<Piece> {
        if (text.isEmpty()) return emptyList()
        val blocks = mutableListOf<Piece>()
        val current = StringBuilder()
        var role = ""
        fun flush() {
            if (current.isEmpty()) return
            blocks.add(Piece(current.toString(), role.ifEmpty { "post" }))
            current.clear()
            role = ""
        }
        for (line in splitLines(text)) {
            val body = line.trimEnd('\r', '\n')
            val kind = lineRole(body)
            val listContinuation = kind == "post" && body.isNotEmpty() &&
                body.first().isWhitespace() && role == "list"
            when {
                kind == "blank" -> {
                    current.append(line)
                    flush()
                }
                kind == "title" -> {
                    flush()
                    blocks.add(Piece(line, "title"))
                }
                kind == "list" -> {
                    if (role == "list") flush()
                    role = "list"
                    current.append(line)
                }
                listContinuation -> current.append(line)
                kind == "quote" -> {
                    if (role != "quote") flush()
                    role = "quote"
                    current.append(line)
                }
                else -> {
                    if (role == "list" || role == "quote") flush()
                    role = "post"
                    current.append(line)
                }
            }
        }
        flush()
        return blocks.flatMap { explode(it, maxChars) }
    }

    private fun explode(piece: Piece, maxChars: Int): List<Piece> {
        if (piece.text.length <= maxChars) return listOf(piece)
        if (piece.role == "code") {
            val lines = splitLines(piece.text)
            if (lines.size > 1) return lines.flatMap { explode(Piece(it, "code"), maxChars) }
        }
        val sentences = splitSentences(piece.text)
        if (sentences.size > 1) {
            return sentences.flatMap { sentence ->
                val child = Piece(sentence, piece.role)
                if (child.text.length > maxChars) hardSplit(child, maxChars) else listOf(child)
            }
        }
        return hardSplit(piece, maxChars)
    }

    private fun splitSentences(text: String): List<String> {
        val urls = URL.findAll(text).map { it.range }.toList()
        val out = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < text.length) {
            if (!inside(urls, i) && isSentenceEnd(text, i)) {
                out.add(text.substring(start, i + 1))
                start = i + 1
            }
            i++
        }
        if (start < text.length) out.add(text.substring(start))
        return out.ifEmpty { listOf(text) }
    }

    private fun isSentenceEnd(text: String, index: Int): Boolean {
        val c = text[index]
        val next = if (index + 1 < text.length) text[index + 1] else ' '
        val prev = if (index > 0) text[index - 1] else ' '
        return when (c) {
            '。', '！', '？', '!', '?' -> true
            '.' -> {
                if (next == '.') return false
                if (prev == '.') return next == ' ' || next == '\n' || next == '\r'
                if (isAbbrev(text, index)) return false
                val prevDigit = prev.isDigit()
                !prevDigit && (next == ' ' || next == '\n' || next == '\r')
            }
            else -> false
        }
    }

    private fun isAbbrev(text: String, dot: Int): Boolean {
        var j = dot - 1
        while (j >= 0 && text[j].isLetter()) j--
        val word = text.substring(j + 1, dot).lowercase()
        if (word in ABBREV) return true
        return word.length == 1 && word[0].isLetter()
    }

    private fun hardSplit(piece: Piece, maxChars: Int): List<Piece> {
        val text = piece.text
        val urls = URL.findAll(text).map { it.range }.toList()
        val out = mutableListOf<Piece>()
        var i = 0
        while (i < text.length) {
            var end = (i + maxChars).coerceAtMost(text.length)
            if (end < text.length) {
                val windowStart = (end - maxChars / 5).coerceAtLeast(i + 1)
                val rel = text.substring(windowStart, end).lastIndexOfAny(
                    charArrayOf(' ', '\n', '\r', '，', ',', '、', '；', ';', '。')
                )
                if (rel >= 0) end = windowStart + rel + 1
            }
            end = snapOutOfUrl(urls, i, end, text.length)
            end = snapSurrogate(text, i, end)
            if (end <= i) end = (i + 1).coerceAtMost(text.length)
            out.add(Piece(text.substring(i, end), piece.role))
            i = end
        }
        return out.ifEmpty { listOf(piece) }
    }

    private fun snapOutOfUrl(urls: List<IntRange>, start: Int, end: Int, length: Int): Int {
        for (range in urls) {
            if (end > range.first && end <= range.last) {
                return if (range.first > start) range.first else (range.last + 1).coerceAtMost(length)
            }
        }
        return end
    }

    private fun snapSurrogate(text: String, start: Int, end: Int): Int {
        if (end in (start + 1) until text.length && text[end].isLowSurrogate()) return end - 1
        return end
    }

    private fun inside(urls: List<IntRange>, index: Int): Boolean =
        urls.any { index >= it.first && index <= it.last }

    private fun splitLines(text: String): List<String> {
        val out = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < text.length) {
            if (text[i] == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
                out.add(text.substring(start, i + 2))
                i += 2
                start = i
                continue
            }
            if (text[i] == '\n' || text[i] == '\r') {
                out.add(text.substring(start, i + 1))
                i += 1
                start = i
                continue
            }
            i++
        }
        if (start < text.length) out.add(text.substring(start))
        return out
    }

    private fun lineRole(body: String): String = when {
        body.isBlank() -> "blank"
        HEAD.containsMatchIn(body) && HEAD.find(body)?.range?.first == 0 -> "title"
        QUOTE.containsMatchIn(body) -> "quote"
        LIST.containsMatchIn(body) -> "list"
        else -> "post"
    }

    private fun atLineStart(text: String, index: Int): Boolean =
        index == 0 || text[index - 1] == '\n' || (text[index - 1] == '\r')

    private fun fenceBlock(text: String, start: Int): Int? {
        if (!text.startsWith("```", start)) return null
        var j = start + 3
        while (j < text.length && text[j] != '\n' && text[j] != '\r') j++
        if (j < text.length && text[j] == '\r') j++
        if (j < text.length && text[j] == '\n') j++
        var k = j
        while (k < text.length) {
            if (atLineStart(text, k) && text.startsWith("```", k)) {
                k += 3
                while (k < text.length && text[k] != '\n' && text[k] != '\r') k++
                if (k < text.length && text[k] == '\r') k++
                if (k < text.length && text[k] == '\n') k++
                return k
            }
            k++
        }
        return text.length
    }

    private fun nextFenceStart(text: String, from: Int): Int {
        var i = from
        while (i < text.length) {
            if (atLineStart(text, i) && text.startsWith("```", i)) return i
            i++
        }
        return text.length
    }
}
