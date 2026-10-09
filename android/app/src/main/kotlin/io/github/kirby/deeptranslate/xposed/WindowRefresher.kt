package io.github.kirby.deeptranslate.xposed

import android.os.Handler
import android.os.Looper
import android.view.View

/**
 * Layout and Compose hooks cannot see the View that owns the text.
 * After a translation lands in cache, ask every window to lay out again
 * so the next StaticLayout / Compose pass reads the cached string.
 */
object WindowRefresher {

    private val handler = Handler(Looper.getMainLooper())
    private val refresh = Runnable {
        scheduled = false
        refreshWindows()
    }
    @Volatile private var scheduled = false

    fun schedule() {
        if (scheduled) return
        scheduled = true
        handler.postDelayed(refresh, 200)
    }

    private fun refreshWindows() {
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
}
