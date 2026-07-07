package io.github.kirby.deeptranslate.xposed

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.libxposed.api.XposedModule
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

class TextBatcher(
    private val module: XposedModule?,
    private val packageName: String
) {
    private val TAG = "DeepTranslate[Batcher]"

    data class PendingItem(val text: String, val onResult: (String) -> Unit)

    private val queue = ConcurrentLinkedQueue<PendingItem>()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val flushRunnable = Runnable { flush() }

    private val batchSize: Int get() = ConfigManager.getBatchSize()
    private val windowMs: Long get() = ConfigManager.getBatchWindowMs()

    @Volatile private var cachedContext: Context? = null

    fun submit(text: String, onResult: (String) -> Unit) {
        if (ConfigManager.isCacheEnabled()) {
            getContext()?.let { TranslationCache.init(it) }
            val cached = TranslationCache.get(text)
            if (cached != null) {
                mainHandler.post { onResult(cached) }
                return
            }
        }

        if (!LanguageDetector.needsTranslation(text, ConfigManager.getTargetLang())) {
            mainHandler.post { onResult(text) }
            return
        }

        queue.add(PendingItem(text, onResult))
        mainHandler.removeCallbacks(flushRunnable)
        if (queue.size >= batchSize) {
            flush()
        } else {
            mainHandler.postDelayed(flushRunnable, windowMs)
        }
    }

    @Synchronized
    private fun flush() {
        if (queue.isEmpty()) return

        val batch = mutableListOf<PendingItem>()
        while (batch.size < batchSize) {
            val item = queue.poll() ?: break
            batch.add(item)
        }
        if (batch.isEmpty()) return

        val texts = batch.map { it.text }.distinct()
        val textToItems = batch.groupBy { it.text }

        executor.execute {
            val uncached = mutableListOf<String>()
            val cachedResults = mutableMapOf<String, String>()
            if (ConfigManager.isCacheEnabled()) {
                getContext()?.let { TranslationCache.init(it) }
                for (text in texts) {
                    val cached = TranslationCache.get(text)
                    if (cached != null) cachedResults[text] = cached
                    else uncached.add(text)
                }
            } else {
                uncached.addAll(texts)
            }

            for ((text, translation) in cachedResults) {
                textToItems[text]?.forEach { mainHandler.post { it.onResult(translation) } }
            }

            if (uncached.isNotEmpty()) {
                val result = TranslationEngine.translateBatch(
                    TranslationEngine.BatchRequest(uncached, packageName), module
                )

                if (result.success) {
                    if (ConfigManager.isCacheEnabled()) {
                        val entries = result.translations.values.map {
                            TranslationCache.CacheEntry(it.original, it.translated, it.sourceLang)
                        }
                        TranslationCache.putBatch(entries, packageName)
                    }
                    for ((original, entry) in result.translations) {
                        textToItems[original]?.forEach { mainHandler.post { it.onResult(entry.translated) } }
                    }
                }

                for (text in uncached) {
                    if (text !in result.translations) {
                        textToItems[text]?.forEach { mainHandler.post { it.onResult(text) } }
                    }
                }
            }
        }

        if (queue.isNotEmpty()) mainHandler.postDelayed(flushRunnable, windowMs)
    }

    private fun getContext(): Context? {
        cachedContext?.let { return it }
        return try {
            val cl = javaClass.classLoader
            val at = Class.forName("android.app.ActivityThread", false, cl)
            (at.getMethod("currentApplication").invoke(null) as? Context)?.also { cachedContext = it }
        } catch (_: Exception) { null }
    }
}
