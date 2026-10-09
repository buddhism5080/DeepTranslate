package io.github.kirby.deeptranslate.xposed

/**
 * Accepts either an OpenAI-compatible base URL or a full chat/models URL.
 *
 *   https://host/v1                      -> …/v1/chat/completions + …/v1/models
 *   https://host/v1/chat/completions    -> itself + …/v1/models
 *   https://host/v1/models              -> …/v1/chat/completions + itself
 */
object OpenAiEndpoints {

    data class Endpoints(val chat: String, val models: String)

    fun resolve(raw: String): Endpoints? {
        var url = raw.trim()
        while (url.endsWith("/")) url = url.dropLast(1)
        if (url.isEmpty() || !url.contains("://")) return null
        return when {
            url.endsWith("/chat/completions") -> {
                val base = url.removeSuffix("/chat/completions")
                Endpoints(chat = url, models = "$base/models")
            }
            url.endsWith("/models") -> {
                val base = url.removeSuffix("/models")
                Endpoints(chat = "$base/chat/completions", models = url)
            }
            else -> Endpoints(
                chat = "$url/chat/completions",
                models = "$url/models",
            )
        }
    }
}
