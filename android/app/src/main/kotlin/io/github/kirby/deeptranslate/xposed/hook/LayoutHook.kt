package io.github.kirby.deeptranslate.xposed.hook

import io.github.kirby.deeptranslate.xposed.ConfigManager
import io.github.kirby.deeptranslate.xposed.TextBatcher
import io.github.kirby.deeptranslate.xposed.TextGate
import io.github.kirby.deeptranslate.xposed.TranslationCache
import io.github.kirby.deeptranslate.xposed.TranslationSession
import io.github.kirby.deeptranslate.xposed.WindowRefresher
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.lang.reflect.Executable
import java.lang.reflect.Modifier

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
            if (!TextGate.shouldTranslate(slice)) return@intercept chain.proceed()

            TranslationCache.ensureFromApp()
            val cached = if (ConfigManager.isCacheEnabled()) TranslationCache.get(slice) else null
            if (cached != null && cached != slice) {
                val replaced = args.toTypedArray()
                replaced[0] = cached
                if (adjustRange) {
                    replaced[1] = 0
                    replaced[2] = cached.length
                }
                return@intercept chain.proceed(replaced)
            }

            batcher.submit(slice) { translated ->
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
