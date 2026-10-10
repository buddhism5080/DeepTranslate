package io.github.kirby.deeptranslate.xposed.hook

import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import android.widget.TextView
import io.github.kirby.deeptranslate.xposed.ConfigManager
import io.github.kirby.deeptranslate.xposed.TextBatcher
import io.github.kirby.deeptranslate.xposed.TextGate
import io.github.kirby.deeptranslate.xposed.TextKinds
import io.github.kirby.deeptranslate.xposed.TranslationSession
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

object TextViewHook : BaseHook() {

    override fun getTag() = "DeepTranslate[TextViewHook]"

    private val pendingTexts = ConcurrentHashMap<String, Boolean>()
    private val applying = ConcurrentHashMap.newKeySet<Int>()
    private val abandoned = ConcurrentHashMap.newKeySet<String>()
    private var batcher: TextBatcher? = null

    override fun onInit(module: XposedModule, param: PackageLoadedParam) {
        if (batcher == null) batcher = TranslationSession.batcher(module, param.packageName)
        hookSetTextMethods(module, param)
        log(module, "TextViewHook active for ${param.packageName}")
    }

    private fun hookSetTextMethods(module: XposedModule, param: PackageLoadedParam) {
        val cl = param.defaultClassLoader
        val textViewClass: Class<*> = try {
            cl.loadClass("android.widget.TextView")
        } catch (e: Exception) {
            logError(module, "TextView class not found: ${e.message}")
            return
        }

        // hook setText(CharSequence)
        try {
            val method = textViewClass.getDeclaredMethod("setText", CharSequence::class.java)
            module.hook(method).intercept { chain ->
                val textView = chain.thisObject as TextView
                val text = chain.args[0] as? CharSequence
                val result = chain.proceed()
                if (applying.contains(System.identityHashCode(textView))) return@intercept result

                if (shouldTranslate(textView, text)) {
                    val textStr = text!!.toString()
                    val key = pendingKey(textView, textStr)
                    if (key !in abandoned && pendingTexts.putIfAbsent(key, true) == null) {
                        val tvRef = WeakReference(textView)
                        batcher?.submit(textStr, TextKinds.of(textStr, textView.javaClass.name, textView.maxLines)) { translated ->
                            release(key) {
                                val tv = tvRef.get() ?: return@release
                                if (tv.text?.toString() != textStr) return@release
                                val saved = tv.text
                                tv.text = copyVisualSpans(saved, translated)
                                checkOverflow(tv, saved)
                            }
                        }
                    }
                }
                result
            }
            log(module, "hooked setText(CharSequence)")
        } catch (e: Exception) {
            logError(module, "hook setText(CharSequence) failed: ${e.message}")
        }

        // hook setText(CharSequence, BufferType)
        try {
            val bufferTypeClass = cl.loadClass("android.widget.TextView\$BufferType")
            val method = textViewClass.getDeclaredMethod("setText", CharSequence::class.java, bufferTypeClass)
            module.hook(method).intercept { chain ->
                val textView = chain.thisObject as TextView
                val text = chain.args[0] as? CharSequence
                val result = chain.proceed()
                if (applying.contains(System.identityHashCode(textView))) return@intercept result

                if (shouldTranslate(textView, text)) {
                    val textStr = text!!.toString()
                    val key = pendingKey(textView, textStr)
                    if (key !in abandoned && pendingTexts.putIfAbsent(key, true) == null) {
                        val tvRef = WeakReference(textView)
                        val bufType = chain.args[1] as TextView.BufferType
                        batcher?.submit(textStr, TextKinds.of(textStr, textView.javaClass.name, textView.maxLines)) { translated ->
                            release(key) {
                                val tv = tvRef.get() ?: return@release
                                if (tv.text?.toString() != textStr) return@release
                                val saved = tv.text
                                tv.setText(copyVisualSpans(saved, translated), bufType)
                                checkOverflow(tv, saved)
                            }
                        }
                    }
                }
                result
            }
            log(module, "hooked setText(CharSequence, BufferType)")
        } catch (e: Exception) {
            logError(module, "hook setText(CharSequence, BufferType) failed: ${e.message}")
        }

        // hook setText(char[], int, int) - no span preservation (raw char array)
        try {
            val method = textViewClass.getDeclaredMethod(
                "setText", CharArray::class.java,
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            )
            module.hook(method).intercept { chain ->
                val textView = chain.thisObject as TextView
                val chars = chain.args[0] as? CharArray
                val start = chain.args[1] as Int
                val len = chain.args[2] as Int
                val result = chain.proceed()
                if (applying.contains(System.identityHashCode(textView))) return@intercept result

                if (chars != null && len > 0 && ConfigManager.isTranslationEnabled() && !isEditText(textView) && !isPasswordField(textView)) {
                    val textStr = String(chars, start, len)
                    if (textStr.length >= 2 && TextGate.shouldTranslate(textStr)) {
                        val key = pendingKey(textView, textStr)
                        if (key !in abandoned && pendingTexts.putIfAbsent(key, true) == null) {
                            val tvRef = WeakReference(textView)
                            batcher?.submit(textStr, TextKinds.of(textStr, textView.javaClass.name, textView.maxLines)) { translated ->
                                release(key) {
                                    val tv = tvRef.get() ?: return@release
                                    if (tv.text?.toString() != textStr) return@release
                                    tv.text = translated
                                    checkOverflow(tv, textStr)
                                }
                            }
                        }
                    }
                }
                result
            }
            log(module, "hooked setText(char[], int, int)")
        } catch (_: Exception) {}
    }

    // ── 溢出检测：中文可能撑破 UI，还原原文 ─────────────────────────────

    private fun pendingKey(tv: TextView, text: String) = "${System.identityHashCode(tv)}_${text.hashCode()}"

    private inline fun release(key: String, block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
        } finally {
            pendingTexts.remove(key)
        }
    }

    /** 翻译后检测文字是否被截断（ellipsize），如果是则还原原文。还原不再送去翻译。 */
    private fun checkOverflow(tv: TextView, original: CharSequence?) {
        if (ConfigManager.isBilingual()) return
        val originalText = original?.toString() ?: return
        tv.post {
            try {
                val layout = tv.layout ?: return@post
                if (layout.lineCount > 0 && layout.getEllipsisCount(layout.lineCount - 1) > 0) {
                    val id = System.identityHashCode(tv)
                    abandoned.add(pendingKey(tv, originalText))
                    applying.add(id)
                    try {
                        tv.text = original
                    } finally {
                        applying.remove(id)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // ── Span 复制：保留原文字样式 ─────────────────────────────────────────

    /**
     * 从原始文字中提取视觉 Span（颜色、粗体、大小、对齐等），
     * 按比例映射到译文字符串上，保留 App 的视觉风格。
     *
     * 排除 ClickableSpan/URLSpan（链接目标已无效）和 ImageSpan（图片不翻译）。
     */
    private fun copyVisualSpans(original: CharSequence?, translated: String): CharSequence {
        if (original !is Spanned || original.isEmpty() || translated.isEmpty()) return translated
        val result = SpannableStringBuilder(translated)
        val origLen = original.length
        val transLen = translated.length

        // 复制段落级 Span
        copyAlignmentSpans(original, result, origLen, transLen)
        copyParagraphSpans(original, result, origLen, transLen)

        // 复制字符级视觉 Span
        val styleSpanClasses = arrayOf(
            ForegroundColorSpan::class.java,
            BackgroundColorSpan::class.java,
            StyleSpan::class.java,
            UnderlineSpan::class.java,
            StrikethroughSpan::class.java,
            AbsoluteSizeSpan::class.java,
            RelativeSizeSpan::class.java,
            TypefaceSpan::class.java,
            TextAppearanceSpan::class.java,
            SubscriptSpan::class.java,
            SuperscriptSpan::class.java,
            SuggestionSpan::class.java
        )

        for (spanClass in styleSpanClasses) {
            val spans = original.getSpans(0, origLen, spanClass)
            for (span in spans) {
                if (span is ImageSpan || span is DynamicDrawableSpan) continue
                val start = original.getSpanStart(span)
                val end = original.getSpanEnd(span)
                val flags = original.getSpanFlags(span)

                // 计算按比例映射的新位置
                val coverRatio = (end - start).toFloat() / origLen
                val newStart: Int
                val newEnd: Int
                if (coverRatio > 0.7f || (start == 0 && end >= origLen)) {
                    newStart = 0
                    newEnd = transLen
                } else {
                    newStart = (start.toFloat() / origLen * transLen).toInt().coerceIn(0, transLen - 1)
                    newEnd = (end.toFloat() / origLen * transLen).toInt().coerceIn(newStart + 1, transLen)
                }

                if (newStart < newEnd && newStart >= 0 && newEnd <= transLen) {
                    try {
                        val cloned = cloneSpan(span) ?: continue
                        result.setSpan(cloned, newStart, newEnd, flags)
                    } catch (_: Exception) {}
                }
            }
        }
        return result
    }

    private fun copyAlignmentSpans(original: Spanned, result: SpannableStringBuilder, origLen: Int, transLen: Int) {
        val alignmentSpans = original.getSpans(0, origLen, AlignmentSpan::class.java)
        for (span in alignmentSpans) {
            try {
                result.setSpan(AlignmentSpan.Standard(span.alignment), 0, transLen, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            } catch (_: Exception) {}
        }
    }

    private fun copyParagraphSpans(original: Spanned, result: SpannableStringBuilder, origLen: Int, transLen: Int) {
        val leadingMarginSpans = original.getSpans(0, origLen, LeadingMarginSpan::class.java)
        for (span in leadingMarginSpans) {
            try {
                result.setSpan(LeadingMarginSpan.Standard(span.getLeadingMargin(true), span.getLeadingMargin(false)),
                    0, transLen, original.getSpanFlags(span))
            } catch (_: Exception) {}
        }
    }

    /** 克隆 Span 对象（不能复用同一个 Span 实例）。 */
    private fun cloneSpan(span: Any): Any? = when (span) {
        is ForegroundColorSpan -> ForegroundColorSpan(span.foregroundColor)
        is BackgroundColorSpan -> BackgroundColorSpan(span.backgroundColor)
        is StyleSpan -> StyleSpan(span.style)
        is UnderlineSpan -> UnderlineSpan()
        is StrikethroughSpan -> StrikethroughSpan()
        is AbsoluteSizeSpan -> AbsoluteSizeSpan(span.size, span.dip)
        is RelativeSizeSpan -> RelativeSizeSpan(span.sizeChange)
        is TypefaceSpan -> TypefaceSpan(span.family ?: "sans-serif")
        is SubscriptSpan -> SubscriptSpan()
        is SuperscriptSpan -> SuperscriptSpan()
        is TextAppearanceSpan -> span // 只读属性，可安全复用
        is SuggestionSpan -> SuggestionSpan(span.localeObject, span.suggestions, span.flags)
        else -> null
    }

    // ── 判断 ────────────────────────────────────────────────────────────────

    private fun shouldTranslate(textView: TextView, text: CharSequence?): Boolean {
        if (!ConfigManager.isTranslationEnabled()) return false
        if (text == null) return false
        val textStr = text.toString()
        if (textStr.isBlank() || textStr.length < 2) return false
        if (isEditText(textView)) return false
        if (isPasswordField(textView)) return false

        if (text is Spannable) {
            val imgSpans = text.getSpans(0, text.length, ImageSpan::class.java)
            if (imgSpans.isNotEmpty()) return false
        }

        if (textStr.matches(Regex("^https?://.*"))) return false
        if (textStr.all { it.isDigit() || it.isWhitespace() }) return false

        return TextGate.shouldTranslate(textStr)
    }

    private fun isEditText(tv: TextView): Boolean = tv is android.widget.EditText

    private fun isPasswordField(tv: TextView): Boolean {
        return try {
            val type = tv.inputType
            (type and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
            (type and android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0 ||
            (type and android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0
        } catch (_: Exception) { false }
    }
}
