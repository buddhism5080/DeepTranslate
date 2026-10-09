package io.github.kirby.deeptranslate.xposed.hook

import android.text.Layout
import android.text.StaticLayout
import android.view.View
import io.github.kirby.deeptranslate.xposed.ConfigManager
import io.github.kirby.deeptranslate.xposed.DisplayText
import io.github.kirby.deeptranslate.xposed.TextGate
import io.github.kirby.deeptranslate.xposed.TextKinds
import io.github.kirby.deeptranslate.xposed.TranslationCache
import io.github.kirby.deeptranslate.xposed.TranslationSession
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.lang.ref.WeakReference

/**
 * React Native's older TextView path is already covered by TextViewHook.
 * Fabric's PreparedLayoutTextView is not a TextView: it draws a layout Fabric
 * already measured. Swap that layout, keeping the same paint and width.
 *
 * Flutter is intentionally not hooked. Its glyphs are drawn inside libflutter.so.
 */
object ReactNativeHook : BaseHook() {

    override fun getTag() = "DeepTranslate[RN]"

    override fun onInit(module: XposedModule, param: PackageLoadedParam) {
        if (!ConfigManager.isTranslationEnabled()) return
        val loader = param.defaultClassLoader
        val legacy = load(loader, "com.facebook.react.views.text.ReactTextView")
        if (legacy != null) log(module, "ReactTextView present; TextView hook covers it")

        val viewClass = load(loader, "com.facebook.react.views.text.PreparedLayoutTextView")
        if (viewClass == null) {
            log(module, "no PreparedLayoutTextView")
            return
        }
        val setter = viewClass.declaredMethods.firstOrNull {
            it.name == "setPreparedLayout" && it.parameterTypes.size == 1
        }
        if (setter == null) {
            logWarn(module, "PreparedLayoutTextView has no setPreparedLayout")
            return
        }
        setter.isAccessible = true
        val batcher = TranslationSession.batcher(module, param.packageName)
        module.hook(setter).intercept { chain ->
            val view = chain.thisObject as? View ?: return@intercept chain.proceed()
            val prepared = chain.args.getOrNull(0) ?: return@intercept chain.proceed()
            val original = textOf(prepared) ?: return@intercept chain.proceed()
            if (!TextGate.shouldTranslate(original)) return@intercept chain.proceed()

            TranslationCache.ensureFromApp()
            val cached = if (ConfigManager.isCacheEnabled()) TranslationCache.get(original) else null
            if (cached != null && cached != original) {
                val shown = DisplayText.present(original, cached, TextKinds.of(original))
                DisplayText.remember(shown)
                val replaced = replaceText(prepared, shown)
                if (replaced != null) {
                    val args = chain.args.toTypedArray()
                    args[0] = replaced
                    return@intercept chain.proceed(args)
                }
            }

            val ref = WeakReference(view)
            batcher.submit(original, TextKinds.of(original)) { translated ->
                if (translated == original) return@submit
                val target = ref.get() ?: return@submit
                target.post {
                    try {
                        val current = readPrepared(target) ?: return@post
                        val currentText = textOf(current) ?: return@post
                        if (currentText != original) return@post
                        val next = replaceText(current, translated) ?: return@post
                        setter.invoke(target, next)
                    } catch (e: Throwable) {
                        logWarn(module, "apply RN layout failed: ${e.message}")
                    }
                }
            }
            chain.proceed()
        }
        log(module, "hooked PreparedLayoutTextView.setPreparedLayout")
    }

    private fun readPrepared(view: View): Any? =
        try {
            view.javaClass.getMethod("getPreparedLayout").invoke(view)
        } catch (_: Throwable) {
            null
        }

    private fun textOf(prepared: Any?): String? {
        if (prepared == null) return null
        return try {
            val layout = prepared.javaClass.getMethod("getLayout").invoke(prepared) as? Layout
            layout?.text?.toString()
        } catch (_: Throwable) {
            null
        }
    }

    private fun replaceText(prepared: Any, newText: String): Any? {
        return try {
            val layout = prepared.javaClass.getMethod("getLayout").invoke(prepared) as? Layout
                ?: return null
            val rebuilt = StaticLayout.Builder.obtain(newText, 0, newText.length, layout.paint, layout.width.coerceAtLeast(0))
                .setAlignment(layout.alignment)
                .setLineSpacing(layout.spacingAdd, layout.spacingMultiplier)
                .setIncludePad(true)
                .setBreakStrategy(layout.breakStrategy)
                .setHyphenationFrequency(layout.hyphenationFrequency)
                .setJustificationMode(layout.justificationMode)
                .build()
            val ctor = prepared.javaClass.declaredConstructors.firstOrNull { it.parameterTypes.size == 6 }
                ?: return null
            ctor.isAccessible = true
            ctor.newInstance(
                rebuilt,
                prepared.javaClass.getMethod("getMaximumNumberOfLines").invoke(prepared),
                prepared.javaClass.getMethod("getVerticalOffset").invoke(prepared),
                prepared.javaClass.getMethod("getReactTags").invoke(prepared),
                prepared.javaClass.getMethod("getTextBreakStrategy").invoke(prepared),
                prepared.javaClass.getMethod("getJustificationMode").invoke(prepared),
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun load(loader: ClassLoader, name: String): Class<*>? =
        try {
            Class.forName(name, false, loader)
        } catch (_: Throwable) {
            null
        }
}
