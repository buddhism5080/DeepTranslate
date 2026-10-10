package io.github.kirby.deeptranslate.xposed

import android.util.Log
import io.github.kirby.deeptranslate.ModuleBroadcast
import io.github.libxposed.api.XposedModule
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

object TranslationEngine {

    private const val TAG = "DeepTranslate[Engine]"

    data class TranslateItem(
        val text: String,
        val role: String = "post",
        val part: Int = 1,
        val parts: Int = 1,
        val before: String = "",
        val after: String = "",
    )

    data class BatchRequest(
        val texts: List<String> = emptyList(),
        val packageName: String,
        val scene: String = "content",
        val items: List<TranslateItem> = emptyList(),
        val targetLang: String = "",
    )

    data class BatchResult(
        val translations: Map<String, TranslationEntry>,
        val success: Boolean,
        val error: String = ""
    )

    data class TranslationEntry(
        val original: String,
        val translated: String,
        val sourceLang: String
    )

    private data class Channel(
        val name: String,
        val chatUrl: String,
        val apiKey: String,
        val model: String,
        val connectMs: Int,
        val readMs: Int,
    )

    private data class ParsedLine(val id: Int, val hasId: Boolean, val translation: String, val lang: String)

    private data class CallResult(
        val translated: Map<String, TranslationEntry>,
        val httpStatus: Int,
        val authFailure: Boolean,
        val errorKind: String = "",
        val errorMessage: String = "",
        val requestBody: String = "",
        val responseBody: String = "",
    )

    fun translateBatch(request: BatchRequest, module: XposedModule?): BatchResult {
        val items = request.items.ifEmpty { request.texts.map { TranslateItem(it) } }
            .filter { it.text.isNotEmpty() }
        if (items.isEmpty()) return BatchResult(emptyMap(), true)
        val pinned = request.copy(targetLang = request.targetLang.ifBlank { ConfigManager.getTargetLang() })

        val primary = channel(
            "primary",
            ConfigManager.getAiUrl(),
            ConfigManager.getAiApiKey(),
            ConfigManager.getAiModel(),
            ConfigManager.getAiConnectTimeout(),
            ConfigManager.getAiReadTimeout(),
        )
        val fallback = channel(
            "fallback",
            ConfigManager.getFallbackUrl(),
            ConfigManager.getFallbackApiKey(),
            ConfigManager.getFallbackModel(),
            ConfigManager.getFallbackConnectTimeout(),
            ConfigManager.getFallbackReadTimeout(),
        )
        val channels = listOfNotNull(primary, fallback)
        if (channels.isEmpty()) return BatchResult(emptyMap(), false, "API URL not configured")

        val done = linkedMapOf<String, TranslationEntry>()
        var pending = items.distinctBy { it.text }
        val backoff = Backoff()
        for (target in channels) {
            if (pending.isEmpty()) break
            backoff.clear()
            val got = drain(target, pending, pinned, module, backoff)
            done.putAll(got)
            pending = pending.filter { it.text !in done }
        }
        val complete = pending.isEmpty()
        if (!complete) {
            module?.log(Log.WARN, TAG, "still missing ${pending.size}/${items.distinctBy { it.text }.size} after retry and fallback")
        }
        return BatchResult(done, complete, if (complete) "" else "incomplete")
    }

    private fun channel(
        name: String,
        url: String,
        apiKey: String,
        model: String,
        connectSeconds: Int,
        readSeconds: Int,
    ): Channel? {
        if (model.isBlank()) return null
        val endpoints = OpenAiEndpoints.resolve(url) ?: return null
        return Channel(
            name,
            endpoints.chat,
            apiKey,
            model,
            connectSeconds.coerceIn(1, 60) * 1000,
            readSeconds.coerceIn(1, 180) * 1000,
        )
    }

    /**
     * Retry the lines still missing, with backoff between tries.
     * [ConfigManager.getRetryCount] is how many retries this channel gets.
     * When they are used up the caller clears backoff and moves to the fallback channel.
     */
    private fun drain(
        target: Channel,
        items: List<TranslateItem>,
        request: BatchRequest,
        module: XposedModule?,
        backoff: Backoff,
    ): Map<String, TranslationEntry> {
        val done = linkedMapOf<String, TranslationEntry>()
        if (items.isEmpty()) return done
        val limit = ConfigManager.getRetryCount()
        var pending = items
        var tries = 0
        while (pending.isNotEmpty()) {
            val call = requestOnce(target, pending, request.scene, request.packageName, request.targetLang, module)
            done.putAll(call.translated)
            if (call.authFailure) {
                module?.log(Log.WARN, TAG, "${target.name} rejected auth, switching channel")
                reportStored(module, request.packageName, call)
                return done
            }
            pending = pending.filter { it.text !in done }
            if (pending.isEmpty()) return done
            if (tries >= limit) {
                reportStored(module, request.packageName, call)
                return done
            }
            tries++
            backoff.pause()
        }
        return done
    }

    private fun reportStored(module: XposedModule?, packageName: String, call: CallResult) {
        if (call.errorKind.isEmpty()) return
        reportError(
            module,
            packageName,
            call.errorKind,
            call.httpStatus,
            call.errorMessage,
            call.requestBody,
            call.responseBody,
        )
    }

    private fun requestOnce(
        target: Channel,
        items: List<TranslateItem>,
        scene: String,
        packageName: String,
        targetLang: String,
        module: XposedModule?,
    ): CallResult {
        val prompt = buildPrompt(targetLang, scene)
        val userContent = buildUserContent(scene, packageName, items, targetLang)
        val body = buildRequestBody(
            target.model, prompt, userContent,
            ConfigManager.getAiTemperature(), ConfigManager.getAiMaxTokens(),
        )
        module?.log(Log.INFO, TAG, "${target.name} ${items.size} ${scene} texts for $packageName")

        try {
            val reply = postOwn(target.chatUrl, body, target.apiKey, target.connectMs, target.readMs)
            val responseCode = reply.code
            val responseBody = reply.body
            if (responseCode !in 200..299) {
                module?.log(Log.ERROR, TAG, "${target.name} HTTP $responseCode: ${responseBody.take(300)}")
                return CallResult(
                    emptyMap(),
                    responseCode,
                    responseCode == 401 || responseCode == 403,
                    "http",
                    "HTTP $responseCode",
                    body,
                    responseBody,
                )
            }
            val translations = parseResponse(responseBody, items.map { it.text })
            if (translations.isEmpty() && items.isNotEmpty()) {
                return CallResult(emptyMap(), responseCode, false, "parse", "响应无法解析成译文", body, responseBody)
            }
            module?.log(Log.INFO, TAG, "${target.name} parsed ${translations.size}/${items.size}")
            sendStatsUpdate(module, translations.size, packageName)
            return CallResult(translations, responseCode, false)
        } catch (e: Exception) {
            module?.log(Log.ERROR, TAG, "${target.name} failed: ${e.message}")
            return CallResult(emptyMap(), 0, false, "network", e.message ?: e.javaClass.simpleName, body, "")
        }
    }

    /** Our own socket. The app closing its client on a page change cannot abort this. */
    private fun postOwn(url: String, json: String, apiKey: String, connectMs: Int, readMs: Int): HttpReply {
        val parsed = URL(url)
        val https = parsed.protocol.equals("https", ignoreCase = true)
        val port = if (parsed.port != -1) parsed.port else if (https) 443 else 80
        val path = buildString {
            append(if (parsed.path.isNullOrEmpty()) "/" else parsed.path)
            if (!parsed.query.isNullOrEmpty()) append('?').append(parsed.query)
        }
        val hostHeader = if (parsed.port == -1) parsed.host else "${parsed.host}:${parsed.port}"
        val started = System.nanoTime()
        val tcp = java.net.Socket()
        tcp.connect(java.net.InetSocketAddress(parsed.host, port), connectMs)
        var active: java.net.Socket = tcp
        try {
            if (https) {
                val used = ((System.nanoTime() - started) / 1_000_000L).toInt()
                val left = (connectMs - used).coerceAtLeast(1)
                val ssl = (javax.net.ssl.SSLSocketFactory.getDefault() as javax.net.ssl.SSLSocketFactory)
                    .createSocket(tcp, parsed.host, port, true) as javax.net.ssl.SSLSocket
                active = ssl
                ssl.soTimeout = left
                ssl.startHandshake()
                val ok = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()
                    .verify(parsed.host, ssl.session)
                if (!ok) throw javax.net.ssl.SSLException("hostname mismatch")
            }
            active.soTimeout = readMs
            val payload = json.toByteArray(Charsets.UTF_8)
            val header = buildString {
                append("POST $path HTTP/1.1\r\n")
                append("Host: $hostHeader\r\n")
                append("Content-Type: application/json\r\n")
                append("Accept: application/json\r\n")
                append("Connection: close\r\n")
                append("Content-Length: ${payload.size}\r\n")
                if (apiKey.isNotEmpty()) append("Authorization: Bearer $apiKey\r\n")
                append("\r\n")
            }
            val out = active.getOutputStream()
            out.write(header.toByteArray(Charsets.US_ASCII))
            out.write(payload)
            out.flush()
            return readHttp(active.getInputStream().buffered())
        } finally {
            try {
                active.close()
            } catch (_: Exception) {
            }
        }
    }

    private data class HttpReply(val code: Int, val body: String)

    private fun readHttp(input: java.io.BufferedInputStream): HttpReply {
        val status = readLine(input)
        val code = status.split(' ').getOrNull(1)?.toIntOrNull() ?: 0
        var length = -1
        var chunked = false
        while (true) {
            val line = readLine(input)
            if (line.isEmpty()) break
            val lower = line.lowercase()
            if (lower.startsWith("content-length:")) length = line.substringAfter(':').trim().toIntOrNull() ?: -1
            if (lower.startsWith("transfer-encoding:") && lower.contains("chunked")) chunked = true
        }
        val body = when {
            chunked -> readChunked(input)
            length >= 0 -> input.readNBytes(length).toString(Charsets.UTF_8)
            else -> input.readBytes().toString(Charsets.UTF_8)
        }
        return HttpReply(code, body)
    }

    private fun readChunked(input: java.io.BufferedInputStream): String {
        val out = java.io.ByteArrayOutputStream()
        while (true) {
            val size = readLine(input).substringBefore(';').trim().toIntOrNull(16) ?: break
            if (size == 0) break
            out.write(input.readNBytes(size))
            readLine(input)
        }
        return out.toString(Charsets.UTF_8)
    }

    private fun readLine(input: java.io.BufferedInputStream): String {
        val buf = java.io.ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0 || b == '\n'.code) break
            if (b != '\r'.code) buf.write(b)
        }
        return buf.toString(Charsets.ISO_8859_1.name())
    }

    private class Backoff {
        private var step = 0

        fun clear() {
            step = 0
        }

        fun pause() {
            val base = ConfigManager.getBackoffBaseMs().coerceAtLeast(100)
            val cap = ConfigManager.getBackoffMaxMs().coerceAtLeast(base)
            val delay = (base.toLong() shl step.coerceAtMost(8)).coerceAtMost(cap.toLong())
            step++
            try {
                Thread.sleep(delay)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun buildPrompt(targetLang: String, scene: String): String {
        val custom = ConfigManager.getAiPrompt().trim()
        val base = if (custom.isNotBlank()) custom else """
你是一个专业翻译引擎。请把待译文本翻译为$targetLang。
结合同一批文本和相邻片段理解语境，不要逐字硬译。
        """.trimIndent()
        val sceneNote = if (scene == "ui") {
            "这批是同一屏幕上的界面文字（按钮、标签、菜单）。译文要短，语气跟控件一致。相邻条目用来消歧，例如 Post 是动词还是名词。"
        } else {
            "这批是正文。role=title 是标题，list 要保留列表符号，quote 要保留引用符，code 只翻译注释和字符串里的自然语言，代码本身原样返回。part 表示同一篇被切开的第几段。"
        }
        return """
$base

场景：$sceneNote
目标语言：$targetLang

输出合同（必须遵守）：
- 只翻译 items[].text。before 和 after 只是相邻原文，禁止写进 translation
- 返回 JSON 数组，每条对应一个 id，不能合并，不能遗漏
- 每项格式 {"id":0,"translation":"译文","lang":"源语言代码"}
- 保持原文的换行、空格、列表和代码围栏
- URL、代码、专有名词、品牌名保持不变
- 如果 text 已经是$targetLang，translation 原样返回
- 不要输出合同之外的说明
        """.trimIndent()
    }

    private fun buildUserContent(scene: String, packageName: String, items: List<TranslateItem>, targetLang: String): String {
        val root = JSONObject()
        root.put("scene", scene)
        root.put("app", packageName)
        root.put("target", targetLang)
        val arr = JSONArray()
        items.forEachIndexed { index, item ->
            val obj = JSONObject()
            obj.put("id", index)
            obj.put("role", item.role)
            if (item.parts > 1) obj.put("part", "${item.part}/${item.parts}")
            if (item.before.isNotBlank()) obj.put("before", item.before)
            obj.put("text", item.text)
            if (item.after.isNotBlank()) obj.put("after", item.after)
            arr.put(obj)
        }
        root.put("items", arr)
        return root.toString()
    }

    private fun buildRequestBody(
        model: String, systemPrompt: String, userContent: String,
        temperature: Double, maxTokens: Int
    ): String {
        val obj = JSONObject()
        obj.put("model", model)
        val messages = JSONArray()
        messages.put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
        messages.put(JSONObject().apply { put("role", "user"); put("content", userContent) })
        obj.put("messages", messages)
        obj.put("temperature", temperature)
        obj.put("max_tokens", maxTokens)
        return obj.toString()
    }

    private fun parseResponse(responseBody: String, originals: List<String>): Map<String, TranslationEntry> {
        val result = linkedMapOf<String, TranslationEntry>()
        try {
            val root = JSONObject(responseBody)
            val message = root.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?: return result
            val content = stripOuterFence(messageContent(message))
            if (content.isBlank()) return result
            val arr = extractArray(content)
            if (arr == null) {
                if (originals.size == 1) {
                    val objectText = translationOfObject(content)
                    if (objectText != null) {
                        result[originals[0]] = TranslationEntry(originals[0], objectText, "auto")
                    } else {
                        val plain = content.trim()
                        if (plain.isNotEmpty() && '{' !in plain && '[' !in plain) {
                            result[originals[0]] = TranslationEntry(originals[0], plain, "auto")
                        }
                    }
                }
                return result
            }
            val rows = ArrayList<ParsedLine>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val translation = item.optString("translation", "").ifBlank { item.optString("translated", "") }
                if (translation.isEmpty()) continue
                val hasId = item.has("id")
                rows.add(ParsedLine(
                    id = if (hasId) item.optInt("id", -1) else -1,
                    hasId = hasId,
                    translation = translation,
                    lang = item.optString("lang", "unknown"),
                ))
            }
            val n = originals.size
            val explicitIds = rows.filter { it.hasId && it.id >= 0 }.map { it.id }
            val oneBased = explicitIds.isNotEmpty() && explicitIds.toSet() == (1..n).toSet()
            val byId = mutableMapOf<Int, TranslationEntry>()
            for ((position, row) in rows.withIndex()) {
                val index = when {
                    oneBased && row.hasId -> row.id - 1
                    row.hasId && !oneBased && 0 !in explicitIds && explicitIds.all { it in 1..n } -> -1
                    row.hasId && row.id in originals.indices -> row.id
                    !row.hasId && explicitIds.isEmpty() && rows.size == n -> position
                    else -> -1
                }
                if (index !in originals.indices || index in byId) continue
                byId[index] = TranslationEntry(originals[index], row.translation, row.lang)
            }
            for ((id, entry) in byId) result[originals[id]] = entry
        } catch (_: Exception) {
        }
        return result
    }

    private fun messageContent(message: JSONObject): String {
        val raw = message.opt("content")
        return when (raw) {
            is String -> raw
            is JSONArray -> buildString {
                for (i in 0 until raw.length()) {
                    val part = raw.optJSONObject(i)
                    append(part?.optString("text").orEmpty().ifBlank { raw.optString(i) })
                }
            }
            else -> message.optString("content", "")
        }
    }

    private fun stripOuterFence(content: String): String {
        val trimmed = content.trim()
        if (!trimmed.startsWith("```")) return trimmed
        val nl = trimmed.indexOf('\n')
        if (nl < 0) return trimmed
        var body = trimmed.substring(nl + 1)
        if (body.trimEnd().endsWith("```")) {
            body = body.trimEnd().removeSuffix("```")
        }
        return body.trim()
    }

    private fun extractArray(content: String): JSONArray? {
        val start = topLevel(content, '[') ?: return null
        if (content.substring(0, start).trimStart().startsWith("{")) return null
        val end = matchingBracket(content, start) ?: return null
        return try {
            JSONArray(content.substring(start, end + 1))
        } catch (_: Exception) {
            null
        }
    }

    private fun translationOfObject(content: String): String? {
        val start = topLevel(content, '{') ?: return null
        val end = matchingBrace(content, start) ?: return null
        return try {
            val obj = JSONObject(content.substring(start, end + 1))
            obj.optString("translation", "").ifBlank { obj.optString("translated", "") }.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun topLevel(text: String, open: Char): Int? {
        var inString = false
        var escaped = false
        for (i in text.indices) {
            val c = text[i]
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') inString = false
                continue
            }
            if (c == '"') {
                inString = true
                continue
            }
            if (c == open) return i
        }
        return null
    }

    private fun matchingBracket(text: String, start: Int): Int? = matching(text, start, '[', ']')

    private fun matchingBrace(text: String, start: Int): Int? = matching(text, start, '{', '}')

    private fun matching(text: String, start: Int, open: Char, close: Char): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') inString = false
                continue
            }
            when (c) {
                '"' -> inString = true
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    /** 网络错误、非 200、以及 200 但解析不出译文，都送到配置 App。不含 API Key。 */
    private fun reportError(
        module: XposedModule?,
        pkg: String,
        kind: String,
        status: Int,
        message: String,
        request: String,
        response: String,
    ) {
        try {
            val ctx = appContext(module) ?: return
            val intent = android.content.Intent("io.github.kirby.deeptranslate.ERROR_LOG").apply {
                setPackage("io.github.kirby.deeptranslate")
                putExtra("package", pkg)
                putExtra("kind", kind)
                putExtra("status", status)
                putExtra("message", message.take(500))
                putExtra("request", redact(request).take(6000))
                putExtra("response", redact(response).take(6000))
            }
            ModuleBroadcast.send(ctx, intent, ConfigManager.getBroadcastToken())
        } catch (e: Exception) {
            module?.log(Log.WARN, TAG, "reportError failed: ${e.message}")
        }
    }

    private fun redact(text: String): String =
        text.replace(Regex("Bearer\\s+\\S+"), "Bearer [redacted]")
            .replace(Regex("sk-[A-Za-z0-9_\\-]{8,}"), "sk-[redacted]")

    private fun appContext(module: XposedModule?): android.content.Context? {
        val m = module ?: return null
        return try {
            val at = Class.forName("android.app.ActivityThread", false, m.javaClass.classLoader)
            at.getMethod("currentApplication").invoke(null) as? android.content.Context
        } catch (_: Exception) {
            null
        }
    }

    /** 通知配置 App 这次新缓存了多少条。 */
    private fun sendStatsUpdate(module: XposedModule?, cached: Int, pkg: String) {
        try {
            val ctx = appContext(module) ?: return

            val intent = android.content.Intent("io.github.kirby.deeptranslate.TOKEN_UPDATE").apply {
                putExtra("cached", cached)
                putExtra("package", pkg)
                setPackage("io.github.kirby.deeptranslate")
            }
            ModuleBroadcast.send(ctx, intent, ConfigManager.getBroadcastToken())
            module?.log(Log.INFO, TAG, "stats update sent: cached=$cached pkg=$pkg")

            if (ConfigManager.isTranslateToast() && cached > 0) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        android.widget.Toast.makeText(
                            ctx, "DeepTranslate: 已翻译 $cached 条",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            module?.log(Log.WARN, TAG, "sendStatsUpdate failed: ${e.message}")
        }
    }
}
