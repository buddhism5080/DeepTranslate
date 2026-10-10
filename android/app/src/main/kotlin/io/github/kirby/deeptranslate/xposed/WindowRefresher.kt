package io.github.kirby.deeptranslate.xposed

import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import io.github.kirby.deeptranslate.xposed.hook.LayoutHook
import java.util.Collections
import java.util.IdentityHashMap
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

    /** Compose keeps the paragraph it already measured. Drop that cache and measure again. */
    private fun dirtyCompose(view: View) {
        val compose = view.javaClass.name.contains("AndroidComposeView")
        if (compose) {
            val root = findRoot(view)
            if (root != null) {
                clearLayoutCaches(root, 0, Collections.newSetFromMap(IdentityHashMap()))
                remeasure(root)
            }
            measureNow(view)
            view.forceLayout()
            view.invalidate()
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i) ?: continue
                dirtyCompose(child)
            }
        }
    }

    private fun findRoot(view: View): Any? {
        val getter = view.javaClass.methods.firstOrNull { it.name == "getRoot" && it.parameterTypes.isEmpty() }
        if (getter != null) {
            val value = try { getter.invoke(view) } catch (_: Throwable) { null }
            if (value != null && hasRemeasure(value)) return value
        }
        for (field in view.javaClass.declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.modifiers)) continue
            field.isAccessible = true
            val value = try { field.get(view) } catch (_: Throwable) { null } ?: continue
            if (hasRemeasure(value)) return value
        }
        return null
    }

    private fun hasRemeasure(value: Any): Boolean =
        value.javaClass.methods.any { it.name.startsWith("requestRemeasure") }

    private fun clearLayoutCaches(node: Any, depth: Int, seen: MutableSet<Any>) {
        if (depth > 10 || !seen.add(node)) return
        nullTextCaches(node)
        var c: Class<*>? = node.javaClass
        while (c != null && c != Any::class.java) {
            for (field in c.declaredFields) {
                if (java.lang.reflect.Modifier.isStatic(field.modifiers)) continue
                val name = field.name
                if (!name.contains("node", true) && !name.contains("cache", true) &&
                    !name.contains("delegate", true) && name != "measurePassDelegate"
                ) continue
                field.isAccessible = true
                val value = try { field.get(node) } catch (_: Throwable) { null } ?: continue
                val type = value.javaClass.name
                if (type.contains("text", true) || type.contains("Paragraph") || type.contains("Cache") || type.contains("Node")) {
                    nullTextCaches(value)
                }
            }
            c = c.superclass
        }
        for (child in childNodes(node)) clearLayoutCaches(child, depth + 1, seen)
    }

    private fun nullTextCaches(obj: Any) {
        val type = obj.javaClass.name
        if (!type.contains("text", true) && !type.contains("Paragraph") && !type.contains("Cache") && !type.contains("LayoutNode")) {
            return
        }
        var c: Class<*>? = obj.javaClass
        while (c != null && c != Any::class.java) {
            for (field in c.declaredFields) {
                if (java.lang.reflect.Modifier.isStatic(field.modifiers) || field.type.isPrimitive) continue
                val name = field.name
                if (name != "layoutCache" && name != "textLayoutResult" && name != "paragraph" &&
                    name != "paragraphIntrinsics" && name != "mLayout"
                ) continue
                field.isAccessible = true
                try { field.set(obj, null) } catch (_: Throwable) {}
            }
            c = c.superclass
        }
    }

    private fun childNodes(node: Any): List<Any> {
        val method = node.javaClass.methods.firstOrNull {
            it.parameterTypes.isEmpty() && (it.name == "getChildren" || it.name.startsWith("getChildren$") || it.name.startsWith("getFoldedChildren"))
        } ?: return emptyList()
        val raw = try { method.invoke(node) } catch (_: Throwable) { null } ?: return emptyList()
        if (raw is Iterable<*>) return raw.filterNotNull()
        val size = raw.javaClass.declaredFields.firstOrNull { it.name == "size" } ?: return emptyList()
        val content = raw.javaClass.declaredFields.firstOrNull { it.name == "content" } ?: return emptyList()
        size.isAccessible = true
        content.isAccessible = true
        val n = (try { size.get(raw) } catch (_: Throwable) { null } as? Int) ?: return emptyList()
        val array = try { content.get(raw) } catch (_: Throwable) { null } as? Array<*> ?: return emptyList()
        return (0 until n.coerceAtMost(array.size)).mapNotNull { array[it] }
    }

    private fun measureNow(view: View) {
        val method = view.javaClass.methods.firstOrNull {
            (it.name == "measureAndLayout" || it.name.startsWith("measureAndLayout$")) &&
                it.parameterTypes.all { type -> type == Boolean::class.javaPrimitiveType }
        } ?: return
        val args = Array(method.parameterTypes.size) { true }
        try { method.invoke(view, *args) } catch (_: Throwable) {}
    }

    private fun remeasure(node: Any) {
        val methods = node.javaClass.methods.filter { it.name.startsWith("requestRemeasure") }
        for (method in methods) {
            val types = method.parameterTypes
            if (types.any { it != Boolean::class.javaPrimitiveType }) continue
            val args: Array<Any> = when (types.size) {
                0 -> emptyArray()
                1 -> arrayOf(false)
                2 -> arrayOf(false, true)
                3 -> arrayOf(false, true, true)
                else -> continue
            }
            try {
                method.isAccessible = true
                method.invoke(node, *args)
            } catch (_: Throwable) {
            }
        }
    }
}
