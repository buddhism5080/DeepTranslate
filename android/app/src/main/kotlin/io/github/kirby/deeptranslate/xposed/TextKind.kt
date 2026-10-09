package io.github.kirby.deeptranslate.xposed

/** UI chrome stays whole. Posts and other long text are content and may be split. */
enum class TextKind { UI, CONTENT }

object TextKinds {
    private val UI_MARKERS = arrayOf(
        "Button", "Chip", "TabLayout", "RadioButton", "CheckBox",
        "Switch", "ToggleButton", "ActionMenuItemView"
    )

    fun of(text: String, className: String? = null, maxLines: Int = -1): TextKind {
        if (text.contains('\n') || text.length > 400) return TextKind.CONTENT
        val chrome = className != null && UI_MARKERS.any { className.contains(it) }
        if (chrome || maxLines == 1 || text.length <= 80) return TextKind.UI
        return TextKind.CONTENT
    }
}
