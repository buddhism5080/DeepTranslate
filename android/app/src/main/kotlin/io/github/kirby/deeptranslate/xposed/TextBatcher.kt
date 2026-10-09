package io.github.kirby.deeptranslate.xposed

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.libxposed.api.XposedModule
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReferenceArray

class TextBatcher(
    private val module: XposedModule?,
    private val packageName: String
) {
    private val tag = "DeepTranslate[Batcher]"

    private class Job(
        val original: String,
        val kind: TextKind,
        val chunks: List<String>,
        val translated: AtomicReferenceArray<String>,
        val left: AtomicInteger,
        val onResult: (String) -> Unit,
    )

    private class Work(val text: String, val job: Job, val index: Int)

    private val uiQueue = ConcurrentLinkedQueue<Work>()
    private val contentQueue = ConcurrentLinkedQueue<Work>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val flushRunnable = Runnable { flush(partial = true) }

    @Volatile private var pool: ExecutorService = newPool(3)
    @Volatile private var poolSize = 3
    @Volatile private var cachedContext: Context? = null

    fun submit(text: String, kind: TextKind = TextKinds.of(text), onResult: (String) -> Unit) {
        if (!TextGate.shouldTranslate(text)) {
            mainHandler.post { onResult(text) }
            return
        }
        if (ConfigManager.isCacheEnabled()) {
            getContext()?.let { TranslationCache.init(it) }
            val cached = TranslationCache.get(text)
            if (cached != null) {
                deliver(text, cached, kind, onResult)
                return
            }
        }

        val chunks = if (kind == TextKind.UI) {
            listOf(text)
        } else {
            TextChunks.split(text, ConfigManager.getMaxChars(), ConfigManager.getMaxParagraphs())
        }
        val hits = if (ConfigManager.isCacheEnabled()) chunks.map { TranslationCache.get(it) } else chunks.map { null }
        if (hits.all { it != null }) {
            val joined = hits.joinToString("") { it!! }
            if (ConfigManager.isCacheEnabled()) TranslationCache.put(text, joined, "cache", packageName)
            deliver(text, joined, kind, onResult)
            return
        }

        val refs = AtomicReferenceArray<String>(chunks.size)
        val missing = mutableListOf<Int>()
        for (i in chunks.indices) {
            val hit = hits[i]
            if (hit != null) refs.set(i, hit) else missing.add(i)
        }
        val job = Job(text, kind, chunks, refs, AtomicInteger(missing.size), onResult)
        val queue = if (kind == TextKind.UI) uiQueue else contentQueue
        for (i in missing) queue.add(Work(chunks[i], job, i))
        if (hasFullBatch()) flush(partial = false) else scheduleWindow()
    }

    @Synchronized
    private fun flush(partial: Boolean) {
        mainHandler.removeCallbacks(flushRunnable)
        val exec = pool()
        var dispatched = false
        while (true) {
            val ui = takeBatch(uiQueue, partial) ?: break
            dispatched = true
            exec.execute { runBatch(ui) }
        }
        while (true) {
            val content = takeBatch(contentQueue, partial) ?: break
            dispatched = true
            exec.execute { runBatch(content) }
        }
        if (!dispatched && partial) return
        if (uiQueue.isNotEmpty() || contentQueue.isNotEmpty()) scheduleWindow()
    }

    private fun scheduleWindow() {
        mainHandler.removeCallbacks(flushRunnable)
        mainHandler.postDelayed(flushRunnable, ConfigManager.getBatchWindowMs())
    }

    private fun hasFullBatch(): Boolean {
        val widgets = ConfigManager.getBatchSize()
        val chars = ConfigManager.getMaxChars()
        return isFull(uiQueue, widgets, chars) || isFull(contentQueue, widgets, chars)
    }

    private fun isFull(queue: ConcurrentLinkedQueue<Work>, maxWidgets: Int, maxChars: Int): Boolean {
        var count = 0
        var chars = 0
        for (work in queue) {
            if (count > 0 && (count >= maxWidgets || chars + work.text.length > maxChars)) return true
            count++
            chars += work.text.length
            if (count >= maxWidgets || chars >= maxChars) return true
        }
        return false
    }

    private fun takeBatch(queue: ConcurrentLinkedQueue<Work>, partial: Boolean): List<Work>? {
        if (queue.isEmpty()) return null
        val maxWidgets = ConfigManager.getBatchSize()
        val maxChars = ConfigManager.getMaxChars()
        if (!partial && !isFull(queue, maxWidgets, maxChars)) return null
        val batch = mutableListOf<Work>()
        var chars = 0
        while (batch.size < maxWidgets) {
            val next = queue.peek() ?: break
            if (batch.isNotEmpty() && chars + next.text.length > maxChars) break
            queue.poll()
            batch.add(next)
            chars += next.text.length
        }
        return batch.ifEmpty { null }
    }

    private fun runBatch(batch: List<Work>) {
        val pending = mutableListOf<Work>()
        if (ConfigManager.isCacheEnabled()) getContext()?.let { TranslationCache.init(it) }
        for (work in batch) {
            val hit = if (ConfigManager.isCacheEnabled()) TranslationCache.get(work.text) else null
            if (hit != null) complete(work, hit, "cache") else pending.add(work)
        }
        if (pending.isEmpty()) return
        val texts = pending.map { it.text }.distinct()
        val result = TranslationEngine.translateBatch(
            TranslationEngine.BatchRequest(texts, packageName), module
        )
        val byText = if (result.success) result.translations else emptyMap()
        for (work in pending) {
            val entry = byText[work.text]
            if (entry != null && entry.translated.isNotEmpty()) {
                complete(work, entry.translated, entry.sourceLang)
            } else {
                if (work.job.left.decrementAndGet() == 0) finish(work.job)
            }
        }
    }

    private fun complete(work: Work, translated: String, sourceLang: String) {
        if (ConfigManager.isCacheEnabled()) {
            TranslationCache.put(work.text, translated, sourceLang, packageName)
        }
        if (translated != work.text) TranslationCache.markOutput(translated)
        work.job.translated.set(work.index, translated)
        if (work.job.left.decrementAndGet() == 0) finish(work.job)
    }

    private fun finish(job: Job) {
        val parts = Array(job.chunks.size) { job.translated.get(it) }
        val complete = parts.all { it != null }
        val joined = buildString {
            for (i in job.chunks.indices) append(parts[i] ?: job.chunks[i])
        }
        if (complete && joined != job.original && ConfigManager.isCacheEnabled()) {
            TranslationCache.put(job.original, joined, "auto", packageName)
        }
        deliver(job.original, joined, job.kind, job.onResult)
    }

    private fun deliver(original: String, translated: String, kind: TextKind, onResult: (String) -> Unit) {
        if (translated != original) TranslationCache.markOutput(translated)
        val shown = DisplayText.present(original, translated, kind)
        if (shown != translated) DisplayText.remember(shown)
        mainHandler.post { onResult(shown) }
    }

    private fun pool(): ExecutorService {
        val want = ConfigManager.getConcurrency()
        if (want == poolSize) return pool
        synchronized(this) {
            val size = ConfigManager.getConcurrency()
            if (size != poolSize) {
                pool.shutdown()
                pool = newPool(size)
                poolSize = size
                module?.log(Log.INFO, tag, "concurrency=$size")
            }
            return pool
        }
    }

    private fun newPool(size: Int): ExecutorService =
        Executors.newFixedThreadPool(size) { runnable ->
            Thread(runnable, "deeptranslate-tr").apply { isDaemon = true }
        }

    private fun getContext(): Context? {
        cachedContext?.let { return it }
        return try {
            val cl = javaClass.classLoader
            val at = Class.forName("android.app.ActivityThread", false, cl)
            (at.getMethod("currentApplication").invoke(null) as? Context)?.also { cachedContext = it }
        } catch (_: Exception) {
            null
        }
    }
}
