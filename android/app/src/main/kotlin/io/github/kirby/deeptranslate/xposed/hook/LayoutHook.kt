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

    override fun onInit(module: XposedModule, param: PackageLoadedParam) {
        if (!ConfigManager.isTranslationEnabled()) return
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
        }
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
            val start = if (adjustRange && args.size >= 3 && args[1] is Int) args[1] as Int else 0
            val end = if (adjustRange && args.size >= 3 && args[2] is Int) args[2] as Int else source.length
            if (start < 0 || end > source.length || start >= end) return@intercept chain.proceed()
            val slice = source.subSequence(start, end).toString()
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
                if (translated != slice) WindowRefresher.schedule()
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
