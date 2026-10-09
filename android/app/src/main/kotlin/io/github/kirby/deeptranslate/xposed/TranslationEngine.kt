package io.github.kirby.deeptranslate.xposed

import android.util.Log
import io.github.libxposed.api.XposedModule
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

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

    private data class Channel(val name: String, val chatUrl: String, val apiKey: String, val model: String)

    private data class ParsedLine(val id: Int, val hasId: Boolean, val translation: String, val lang: String)

    private data class CallResult(
        val translated: Map<String, TranslationEntry>,
        val httpStatus: Int,
        val authFailure: Boolean,
    )

    fun translateBatch(request: BatchRequest, module: XposedModule?): BatchResult {
        val items = request.items.ifEmpty { request.texts.map { TranslateItem(it) } }
            .filter { it.text.isNotEmpty() }
        if (items.isEmpty()) return BatchResult(emptyMap(), true)

        val primary = channel("primary", ConfigManager.getAiUrl(), ConfigManager.getAiApiKey(), ConfigManager.getAiModel())
        val fallback = channel(
            "fallback",
            ConfigManager.getFallbackUrl(),
            ConfigManager.getFallbackApiKey(),
            ConfigManager.getFallbackModel(),
        )
        val channels = listOfNotNull(primary, fallback)
        if (channels.isEmpty()) return BatchResult(emptyMap(), false, "API URL not configured")

        val done = linkedMapOf<String, TranslationEntry>()
        var pending = items.distinctBy { it.text }
        for (target in channels) {
            if (pending.isEmpty()) break
            val got = drain(target, pending, request, module)
            done.putAll(got)
            pending = pending.filter { it.text !in done }
        }
        val complete = pending.isEmpty()
        if (!complete) {
            module?.log(Log.WARN, TAG, "still missing ${pending.size}/${items.distinctBy { it.text }.size} after retry and fallback")
        }
        return BatchResult(done, complete, if (complete) "" else "incomplete")
    }

    private fun channel(name: String, url: String, apiKey: String, model: String): Channel? {
        if (model.isBlank()) return null
        val endpoints = OpenAiEndpoints.resolve(url) ?: return null
        return Channel(name, endpoints.chat, apiKey, model)
    }

    /** One batch attempt, then each still-missing line on its own. A shared counter must not let one failure eat the others. */
    private fun drain(
        target: Channel,
        items: List<TranslateItem>,
        request: BatchRequest,
        module: XposedModule?,
    ): Map<String, TranslationEntry> {
        val done = linkedMapOf<String, TranslationEntry>()
        if (items.isEmpty()) return done
        val first = requestOnce(target, items, request.scene, request.packageName, module)
        done.putAll(first.translated)
        if (first.authFailure) {
            module?.log(Log.WARN, TAG, "${target.name} rejected auth, switching channel")
            return done
        }
        val missed = items.filter { it.text !in done }
        if (missed.isEmpty()) return done
        val solos = if (items.size > 1 && first.httpStatus in 200..299) {
            maxOf(1, ConfigManager.getRetryCount())
        } else {
            ConfigManager.getRetryCount()
        }
        for (item in missed) {
            var attempt = 0
            while (attempt < solos && item.text !in done) {
                if (attempt > 0) pause(attempt)
                attempt++
                val call = requestOnce(target, listOf(item), request.scene, request.packageName, module)
                if (call.authFailure) {
                    module?.log(Log.WARN, TAG, "${target.name} rejected auth, switching channel")
                    return done
                }
                call.translated[item.text]?.let { done[item.text] = it }
                if (item.text !in done && call.httpStatus == 429) pause(attempt)
            }
        }
        return done
    }

    private fun pause(attempt: Int) {
        try {
            Thread.sleep(200L * attempt)
        } catch (_: InterruptedException) {
        }
    }

    private fun requestOnce(
        target: Channel,
        items: List<TranslateItem>,
        scene: String,
        packageName: String,
        module: XposedModule?,
    ): CallResult {
        val timeout = ConfigManager.getAiTimeout()
        val prompt = buildPrompt(ConfigManager.getTargetLang(), scene)
        val userContent = buildUserContent(scene, packageName, items)
        val body = buildRequestBody(
            target.model, prompt, userContent,
            ConfigManager.getAiTemperature(), ConfigManager.getAiMaxTokens(),
        )
        module?.log(Log.INFO, TAG, "${target.name} ${items.size} ${scene} texts for $packageName")

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(target.chatUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TimeUnit.SECONDS.toMillis(timeout.toLong()).toInt()
                readTimeout = TimeUnit.SECONDS.toMillis(timeout.toLong()).toInt()
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                if (target.apiKey.isNotEmpty()) setRequestProperty("Authorization", "Bearer ${target.apiKey}")
            }
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
                writer.flush()
            }
            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            }
            if (responseCode !in 200..299) {
                module?.log(Log.ERROR, TAG, "${target.name} HTTP $responseCode: ${responseBody.take(300)}")
                return CallResult(emptyMap(), responseCode, responseCode == 401 || responseCode == 403)
            }
            val translations = parseResponse(responseBody, items.map { it.text })
            module?.log(Log.INFO, TAG, "${target.name} parsed ${translations.size}/${items.size}")
            sendStatsUpdate(module, translations.size, packageName)
            return CallResult(translations, responseCode, false)
        } catch (e: Exception) {
            module?.log(Log.ERROR, TAG, "${target.name} failed: ${e.message}")
            return CallResult(emptyMap(), 0, false)
        } finally {
            connection?.disconnect()
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

    private fun buildUserContent(scene: String, packageName: String, items: List<TranslateItem>): String {
        val root = JSONObject()
        root.put("scene", scene)
        root.put("app", packageName)
        root.put("target", ConfigManager.getTargetLang())
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
            val content = messageContent(message)
            if (content.isBlank()) return result
            val arr = extractArray(content)
            if (arr == null) {
                if (originals.size == 1) {
                    val plain = content.replace("```json", "").replace("```", "").trim()
                    if (plain.isNotEmpty() && !plain.startsWith("{") && !plain.startsWith("[")) {
                        result[originals[0]] = TranslationEntry(originals[0], plain, "auto")
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
            val oneBased = explicitIds.isNotEmpty() &&
                0 !in explicitIds &&
                explicitIds.all { it in 1..n } &&
                explicitIds.maxOrNull() == n
            val byId = mutableMapOf<Int, TranslationEntry>()
            for ((position, row) in rows.withIndex()) {
                val index = when {
                    oneBased && row.hasId -> row.id - 1
                    row.hasId && row.id in originals.indices -> row.id
                    !row.hasId && rows.size == n -> position
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

    private fun extractArray(content: String): JSONArray? {
        val cleaned = content.replace("```json", "").replace("```", "").trim()
        val start = cleaned.indexOf('[')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until cleaned.length) {
            val c = cleaned[i]
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') inString = false
                continue
            }
            when (c) {
                '"' -> inString = true
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        return try {
                            JSONArray(cleaned.substring(start, i + 1))
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }
        }
        return null
    }

    /** 通知配置 App 这次新缓存了多少条。 */
    private fun sendStatsUpdate(module: XposedModule?, cached: Int, pkg: String) {
        try {
            val ctx = module?.let { m ->
                val cl = m.javaClass.classLoader
                val at = Class.forName("android.app.ActivityThread", false, cl)
                (at.getMethod("currentApplication").invoke(null) as? android.content.Context)
            } ?: return

            val intent = android.content.Intent("io.github.kirby.deeptranslate.TOKEN_UPDATE").apply {
                putExtra("cached", cached)
                putExtra("package", pkg)
                setPackage("io.github.kirby.deeptranslate")
            }
            ctx.sendBroadcast(intent)
            module.log(Log.INFO, TAG, "stats update sent: cached=$cached pkg=$pkg")

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
