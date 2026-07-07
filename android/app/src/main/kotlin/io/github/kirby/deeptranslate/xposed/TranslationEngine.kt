package io.github.kirby.deeptranslate.xposed

import android.util.Log
import io.github.libxposed.api.XposedModule
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit

object TranslationEngine {

    private const val TAG = "DeepTranslate[Engine]"

    data class BatchRequest(val texts: List<String>, val packageName: String)

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

    fun translateBatch(request: BatchRequest, module: XposedModule?): BatchResult {
        if (request.texts.isEmpty()) return BatchResult(emptyMap(), true)

        val url = ConfigManager.getAiUrl()
        val apiKey = ConfigManager.getAiApiKey()
        val model = ConfigManager.getAiModel()
        val targetLang = ConfigManager.getTargetLang()
        val timeout = ConfigManager.getAiTimeout()
        val temperature = ConfigManager.getAiTemperature()
        val maxTokens = ConfigManager.getAiMaxTokens()

        if (url.isEmpty()) return BatchResult(emptyMap(), false, "API URL not configured")
        if (apiKey.isEmpty()) return BatchResult(emptyMap(), false, "API key not configured")

        val prompt = buildPrompt(targetLang)
        val userContent = buildUserContent(request.texts)
        val requestBody = buildRequestBody(model, prompt, userContent, temperature, maxTokens)

        module?.log(Log.INFO, TAG, "translating ${request.texts.size} texts for ${request.packageName}")

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TimeUnit.SECONDS.toMillis(timeout.toLong()).toInt()
                readTimeout = TimeUnit.SECONDS.toMillis(timeout.toLong()).toInt()
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            }

            if (responseCode !in 200..299) {
                module?.log(Log.ERROR, TAG, "API error $responseCode: ${responseBody.take(500)}")
                return BatchResult(emptyMap(), false, "HTTP $responseCode")
            }

            val translations = parseResponse(responseBody, request.texts)
            module?.log(Log.INFO, TAG, "translated ${translations.size}/${request.texts.size} texts")

            // 解析 token 用量，发送广播给配置 App 累计
            try {
                val root = JSONObject(responseBody)
                val usage = root.optJSONObject("usage")
                val totalTokens = usage?.optInt("total_tokens", 0) ?: 0
                val cachedCount = translations.size
                sendStatsUpdate(module, totalTokens, cachedCount, request.packageName)
            } catch (_: Exception) {}

            return BatchResult(translations, true)

        } catch (e: Exception) {
            module?.log(Log.ERROR, TAG, "translateBatch failed: ${e.message}")
            return BatchResult(emptyMap(), false, e.message ?: "unknown error")
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildPrompt(targetLang: String): String {
        val customPrompt = ConfigManager.getAiPrompt()
        if (customPrompt.isNotBlank()) return customPrompt
        return """
你是一个专业翻译引擎。请将用户提供的文本翻译为$targetLang。

规则：
1. 自动识别每条文本的源语言
2. 结合整批文本的上下文理解语境，确保翻译准确自然
3. 不要逐字翻译，要理解完整语义后再翻译
4. 保持原文的格式
5. 如果文本已经是$targetLang，原样返回
6. 专有名词、品牌名、代码、URL保持不变
7. 只返回 JSON 数组

输出格式： [{"id":0,"translation":"译文","lang":"源语言代码"}]
        """.trimIndent()
    }

    private fun buildUserContent(texts: List<String>): String {
        val arr = JSONArray()
        for ((index, text) in texts.withIndex()) {
            val obj = JSONObject()
            obj.put("id", index)
            obj.put("text", text)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun buildRequestBody(
        model: String, systemPrompt: String, userContent: String,
        temperature: Double, maxTokens: Int
    ): String {
        val obj = JSONObject()
        obj.put("model", model)
        val messages = JSONArray()
        val sysMsg = JSONObject().apply { put("role", "system"); put("content", systemPrompt) }
        messages.put(sysMsg)
        val userMsg = JSONObject().apply { put("role", "user"); put("content", userContent) }
        messages.put(userMsg)
        obj.put("messages", messages)
        obj.put("temperature", temperature)
        obj.put("max_tokens", maxTokens)
        return obj.toString()
    }

    private fun parseResponse(responseBody: String, originals: List<String>): Map<String, TranslationEntry> {
        val result = mutableMapOf<String, TranslationEntry>()
        try {
            val root = JSONObject(responseBody)
            val choices = root.optJSONArray("choices") ?: return result
            if (choices.length() == 0) return result
            val message = choices.getJSONObject(0).optJSONObject("message") ?: return result
            val content = message.optString("content", "").replace("```json", "").replace("```", "").trim()
            if (content.isBlank()) return result

            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val id = item.optInt("id", -1)
                val translation = item.optString("translation", "")
                val lang = item.optString("lang", "unknown")
                if (id in originals.indices && translation.isNotEmpty()) {
                    result[originals[id]] = TranslationEntry(originals[id], translation, lang)
                }
            }
        } catch (e: Exception) {
            if (originals.size == 1) {
                try {
                    val content = JSONObject(responseBody)
                        .getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content").trim()
                    if (content.isNotEmpty()) result[originals[0]] = TranslationEntry(originals[0], content, "auto")
                } catch (_: Exception) {}
            }
        }
        return result
    }

    /** 发送广播通知配置 App 累计 token 消耗和缓存条数。 */
    private fun sendStatsUpdate(module: XposedModule?, tokens: Int, cached: Int, pkg: String) {
        try {
            val ctx = module?.let { m ->
                val cl = m.javaClass.classLoader
                val at = Class.forName("android.app.ActivityThread", false, cl)
                (at.getMethod("currentApplication").invoke(null) as? android.content.Context)
            } ?: return

            val intent = android.content.Intent("io.github.kirby.deeptranslate.TOKEN_UPDATE").apply {
                putExtra("tokens", tokens)
                putExtra("cached", cached)
                putExtra("package", pkg)
                setPackage("io.github.kirby.deeptranslate")
            }
            ctx.sendBroadcast(intent)
            module.log(Log.INFO, TAG, "stats update sent: tokens=$tokens cached=$cached pkg=$pkg")

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
