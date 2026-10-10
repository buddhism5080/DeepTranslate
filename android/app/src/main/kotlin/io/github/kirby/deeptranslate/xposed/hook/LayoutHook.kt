package io.github.kirby.deeptranslate.xposed.hook

import io.github.kirby.deeptranslate.xposed.ConfigManager
import io.github.kirby.deeptranslate.xposed.DisplayText
import io.github.kirby.deeptranslate.xposed.TextBatcher
import io.github.kirby.deeptranslate.xposed.TextGate
import io.github.kirby.deeptranslate.xposed.TextKinds
import io.github.kirby.deeptranslate.xposed.TranslationCache
import io.github.kirby.deeptranslate.xposed.TranslationSession
import io.github.kirby.deeptranslate.xposed.WindowRefresher
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.lang.reflect.Executable
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Text that never goes through TextView.setText:
 * StaticLayout / BoringLayout, and Compose TextLayout when the app ships it.
 * Cache hits replace the CharSequence before layout. Misses are prefetched and
 * the window is laid out again.
 */
object LayoutHook : BaseHook() {

    override fun getTag() = "DeepTranslate[Layout]"

    private val composeClasses = listOf(
        "androidx.compose.ui.text.android.TextLayout",
        "androidx.compose.ui.text.platform.AndroidParagraphIntrinsics",
    )
    private val shown = Collections.synchronizedMap(object : LinkedHashMap<String, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 400
    })
    private val inflight = ConcurrentHashMap.newKeySet<String>()
    private val held = ConcurrentHashMap.newKeySet<String>()

    fun hold(text: String) {
        if (text.isNotEmpty()) held.add(text)
    }

    fun isHeld(text: String) = text in held

    override fun onInit(module: XposedModule, param: PackageLoadedParam) {
        val batcher = TranslationSession.batcher(module, param.packageName)
        val loader = param.defaultClassLoader
        hookLayoutClass(module, loader, "android.text.StaticLayout\$Builder", "obtain", adjustRange = true, batcher)
        hookBoringLayout(module, loader, batcher)
        hookCompose(module, loader, batcher)
        log(module, "layout hooks installed for ${param.packageName}")
    }

    private fun hookLayoutClass(
        module: XposedModule,
        loader: ClassLoader,
        className: String,
        methodName: String,
        adjustRange: Boolean,
        batcher: TextBatcher,
    ) {
        val clazz = load(loader, className) ?: return
        val methods = clazz.declaredMethods.filter { it.name == methodName && it.parameterTypes.firstOrNull() == CharSequence::class.java }
        if (methods.isEmpty()) {
            logWarn(module, "no $methodName(CharSequence, ...) on $className")
            return
        }
        for (method in methods) {
            hookExecutable(module, method, adjustRange, batcher, layoutPref = true)
            log(module, "hooked $className.$methodName")
        }
    }

    private fun hookBoringLayout(module: XposedModule, loader: ClassLoader, batcher: TextBatcher) {
        val clazz = load(loader, "android.text.BoringLayout") ?: return
        val methods = clazz.declaredMethods.filter {
            it.name == "make" &&
                Modifier.isStatic(it.modifiers) &&
                it.parameterTypes.firstOrNull() == CharSequence::class.java
        }
        for (method in methods) {
            hookExecutable(module, method, adjustRange = false, batcher, layoutPref = true)
        }
        if (methods.isNotEmpty()) log(module, "hooked BoringLayout.make x${methods.size}")
    }

    private fun hookCompose(module: XposedModule, loader: ClassLoader, batcher: TextBatcher) {
        for (name in composeClasses) {
            val clazz = load(loader, name) ?: continue
            val ctors = clazz.declaredConstructors.filter {
                it.parameterTypes.firstOrNull()?.let { p -> CharSequence::class.java.isAssignableFrom(p) } == true
            }
            for (ctor in ctors) {
                hookExecutable(module, ctor, adjustRange = false, batcher, layoutPref = false)
            }
            if (ctors.isNotEmpty()) log(module, "hooked $name constructors x${ctors.size}")
            if (name.endsWith("TextLayout")) hookPaint(module, clazz)
        }
    }

    /** 已经画在屏幕上的 Compose 文字不会重建。下一帧用译文重画。 */
    private fun hookPaint(module: XposedModule, clazz: Class<*>) {
        val paints = clazz.declaredMethods.filter { method ->
            method.name == "paint" && method.parameterTypes.any {
                it.name == "android.graphics.Canvas" || it.name.endsWith(".Canvas")
            }
        }
        for (method in paints) {
            module.hook(method).intercept { chain ->
                if (!ConfigManager.isTranslationEnabled() || !ConfigManager.isHookCompose()) {
                    return@intercept chain.proceed()
                }
                val layout = chain.thisObject ?: return@intercept chain.proceed()
                val text = textOf(layout) ?: return@intercept chain.proceed()
                if (TranslationCache.peekOutput(text)) return@intercept chain.proceed()
                val cached = TranslationCache.peek(text) ?: return@intercept chain.proceed()
                if (cached == text) return@intercept chain.proceed()
                val canvas = chain.args.firstNotNullOfOrNull { androidCanvas(it) }
                    ?: return@intercept chain.proceed()
                val paint = paintOf(layout) ?: return@intercept chain.proceed()
                val width = widthOf(layout)
                if (width <= 0) return@intercept chain.proceed()
                val shown = DisplayText.present(text, cached, TextKinds.of(text))
                DisplayText.remember(shown)
                canvas.save()
                try {
                    val built = android.text.StaticLayout.Builder
                        .obtain(shown, 0, shown.length, paint, width)
                        .build()
                    built.draw(canvas)
                } finally {
                    canvas.restore()
                }
                null
            }
            log(module, "hooked ${clazz.name}.${method.name}")
        }
    }

    private val textFields = java.util.concurrent.ConcurrentHashMap<Class<*>, java.lang.reflect.Field>()
    private val noText = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<Class<*>, Boolean>())

    private fun textOf(layout: Any): String? {
        val type = layout.javaClass
        if (type in noText) return null
        val field = textFields[type] ?: run {
            var found: java.lang.reflect.Field? = null
            var best = -1
            var c: Class<*>? = type
            while (c != null && c != Any::class.java) {
                for (candidate in c.declaredFields) {
                    if (candidate.type != String::class.java && candidate.type != CharSequence::class.java) continue
                    candidate.isAccessible = true
                    val value = try { candidate.get(layout)?.toString() } catch (_: Throwable) { null } ?: continue
                    if (value.length > best) {
                        best = value.length
                        found = candidate
                    }
                }
                c = c.superclass
            }
            if (found == null) {
                noText.add(type)
                return null
            }
            textFields[type] = found
            found
        }
        return try { field.get(layout)?.toString() } catch (_: Throwable) { null }
    }

    private fun paintOf(layout: Any): android.text.TextPaint? {
        var c: Class<*>? = layout.javaClass
        while (c != null && c != Any::class.java) {
            for (field in c.declaredFields) {
                if (!android.text.TextPaint::class.java.isAssignableFrom(field.type)) continue
                field.isAccessible = true
                return try { field.get(layout) as? android.text.TextPaint } catch (_: Throwable) { null }
            }
            c = c.superclass
        }
        return null
    }

    private fun widthOf(layout: Any): Int {
        val method = layout.javaClass.methods.firstOrNull { it.name == "getWidth" && it.parameterTypes.isEmpty() }
        val value = try { method?.invoke(layout) as? Int } catch (_: Throwable) { null }
        return value ?: 0
    }

    private fun androidCanvas(value: Any?): android.graphics.Canvas? {
        if (value is android.graphics.Canvas) return value
        if (value == null || !value.javaClass.name.endsWith("Canvas")) return null
        val method = value.javaClass.methods.firstOrNull { it.name == "getNativeCanvas" && it.parameterTypes.isEmpty() }
        return try { method?.invoke(value) as? android.graphics.Canvas } catch (_: Throwable) { null }
    }

    private fun hookExecutable(
        module: XposedModule,
        executable: Executable,
        adjustRange: Boolean,
        batcher: TextBatcher,
        layoutPref: Boolean,
    ) {
        module.hook(executable).intercept { chain ->
            fun swapped(displayed: String): Any? {
                val replaced = chain.args.toTypedArray()
                replaced[0] = displayed
                if (adjustRange && replaced.size >= 3) {
                    replaced[1] = 0
                    replaced[2] = displayed.length
                }
                return chain.proceed(replaced)
            }
            if (!ConfigManager.isTranslationEnabled()) return@intercept chain.proceed()
            val enabled = if (layoutPref) ConfigManager.isHookLayout() else ConfigManager.isHookCompose()
            if (!enabled) return@intercept chain.proceed()
            val args = chain.args
            if (args.isEmpty()) return@intercept chain.proceed()
            val source = args[0] as? CharSequence ?: return@intercept chain.proceed()
            if (source is android.text.Editable) return@intercept chain.proceed()
            val start = if (adjustRange && args.size >= 3 && args[1] is Int) args[1] as Int else 0
            val end = if (adjustRange && args.size >= 3 && args[2] is Int) args[2] as Int else source.length
            if (start < 0 || end > source.length || start >= end) return@intercept chain.proceed()
            val slice = source.subSequence(start, end).toString()
            if (LayoutHook.isHeld(slice)) return@intercept chain.proceed()
            val key = "${ConfigManager.getTargetLang()}\u0000${ConfigManager.isBilingual()}\u0000$slice"
            // Span ranges on AndroidParagraphIntrinsics are indexes into the original string.
            if (executable.declaringClass.name.endsWith("AndroidParagraphIntrinsics")) {
                return@intercept chain.proceed()
            }
            shown[key]?.let { remembered ->
                return@intercept swapped(remembered)
            }
            if (!inflight.add(key)) return@intercept chain.proceed()
            if (!TextGate.shouldTranslate(slice)) {
                inflight.remove(key)
                return@intercept chain.proceed()
            }

            TranslationCache.ensureFromApp()
            val kind = TextKinds.of(slice)
            val cached = if (ConfigManager.isCacheEnabled()) TranslationCache.get(slice) else null
            if (cached != null) {
                inflight.remove(key)
                if (cached == slice) return@intercept chain.proceed()
                val displayed = DisplayText.present(slice, cached, kind)
                DisplayText.remember(displayed)
                shown[key] = displayed
                return@intercept swapped(displayed)
            }

            batcher.submit(slice, kind) { translated ->
                inflight.remove(key)
                shown[key] = translated
                if (translated != slice) WindowRefresher.schedule(slice, translated)
            }
            chain.proceed()
        }
    }

    private fun load(loader: ClassLoader, name: String): Class<*>? =
        try {
            Class.forName(name, false, loader)
        } catch (_: Throwable) {
            null
        }
}
