package io.github.kirby.deeptranslate.xposed

import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import io.github.kirby.deeptranslate.xposed.hook.LayoutHook
import java.util.concurrent.ConcurrentHashMap

/**
 * Layout and Compose keep the text they already measured.
 * After a translation is cached, push it onto visible TextViews and mark
 * Compose nodes dirty so the next measure rebuilds those layouts.
 */
object WindowRefresher {

    private val handler = Handler(Looper.getMainLooper())
    private val pending = ConcurrentHashMap<String, String>()
    private val refresh = Runnable {
        scheduled = false
        val batch = HashMap(pending)
        for ((key, value) in batch) pending.remove(key, value)
        refreshWindows(batch)
    }
    @Volatile private var scheduled = false

    fun schedule(original: String, displayed: String) {
        if (original.isEmpty() || displayed.isEmpty() || original == displayed) return
        pending[original] = displayed
        if (scheduled) return
        scheduled = true
        handler.postDelayed(refresh, 200)
    }

    private fun refreshWindows(batch: Map<String, String>) {
        if (batch.isEmpty() || !ConfigManager.isTranslationEnabled()) return
        try {
            val wmgClass = Class.forName("android.view.WindowManagerGlobal")
            val wmg = wmgClass.getMethod("getInstance").invoke(null) ?: return
            val field = wmgClass.getDeclaredField("mViews")
            field.isAccessible = true
            val views = field.get(wmg) as? Iterable<*> ?: return
            for (view in views) {
                if (view is View) {
                    view.post {
                        try {
                            applyToTree(view, batch)
                            dirtyCompose(view)
                            view.requestLayout()
                            view.invalidate()
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun applyToTree(view: View, batch: Map<String, String>) {
        if (view is TextView && view !is EditText && !isPassword(view)) {
            val current = view.text?.toString()
            if (current != null && !LayoutHook.isHeld(current)) {
                val displayed = batch[current]
                if (displayed != null) {
                    DisplayText.remember(displayed)
                    view.text = displayed
                }
            }
        }
        view.forceLayout()
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i) ?: continue
                applyToTree(child, batch)
            }
        }
    }

    private fun isPassword(view: TextView): Boolean {
        val variation = view.inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
    }

    /** Compose keeps the paragraph it already measured. Mark the root dirty with the current method shape. */
    private fun dirtyCompose(view: View) {
        if (view.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView") {
            val root = view.javaClass.declaredFields
                .firstOrNull { it.name == "root" }
                ?.also { it.isAccessible = true }
                ?.get(view)
            if (root != null) remeasure(root)
            view.forceLayout()
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i) ?: continue
                dirtyCompose(child)
            }
        }
    }

    private fun remeasure(node: Any) {
        val method = node.javaClass.methods.firstOrNull { it.name.startsWith("requestRemeasure") } ?: return
        val booleans = method.parameterTypes.count { it == Boolean::class.javaPrimitiveType }
        try {
            when (booleans) {
                0 -> method.invoke(node)
                1 -> method.invoke(node, true)
                2 -> method.invoke(node, true, true)
                else -> method.invoke(node, true, true, true)
            }
        } catch (_: Throwable) {
        }
    }
}
