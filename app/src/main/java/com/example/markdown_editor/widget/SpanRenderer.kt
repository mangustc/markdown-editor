package com.example.markdown_editor.widget

import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.style.*
import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.domain.model.TokenType

object SpanRenderer {
    fun apply(editable: Editable, spans: List<SpanInfo>) {
        editable.getSpans(0, editable.length, Any::class.java)
            .filter {
                it is StyleSpan || it is RelativeSizeSpan ||
                        it is ForegroundColorSpan || it is TypefaceSpan ||
                        it is ImageSpan || it is BackgroundColorSpan
            }
            .forEach { editable.removeSpan(it) }

        spans.forEach { info ->
            if (info.start >= info.end || info.end > editable.length) return@forEach
            setSpansFor(editable, info)
        }
    }

    private fun setSpansFor(e: Editable, info: SpanInfo) {
        val s = info.start; val end = info.end
        val F = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        when (info.type) {
            TokenType.H1 -> {
                e.setSpan(StyleSpan(Typeface.BOLD), s, end, F)
                e.setSpan(RelativeSizeSpan(2.0f), s, end, F)
                e.setSpan(ForegroundColorSpan(Color.parseColor("#1A73E8")), s, end, F)
            }
            TokenType.H2 -> {
                e.setSpan(StyleSpan(Typeface.BOLD), s, end, F)
                e.setSpan(RelativeSizeSpan(1.5f), s, end, F)
            }
            TokenType.H3 -> {
                e.setSpan(StyleSpan(Typeface.BOLD), s, end, F)
                e.setSpan(RelativeSizeSpan(1.2f), s, end, F)
            }
            TokenType.BOLD ->
                e.setSpan(StyleSpan(Typeface.BOLD), s, end, F)
            TokenType.ITALIC ->
                e.setSpan(StyleSpan(Typeface.ITALIC), s, end, F)
            TokenType.CODE_INLINE -> {
                e.setSpan(TypefaceSpan("monospace"), s, end, F)
                e.setSpan(BackgroundColorSpan(Color.parseColor("#F5F5F5")), s, end, F)
                e.setSpan(ForegroundColorSpan(Color.parseColor("#C7254E")), s, end, F)
            }
            TokenType.CODE_BLOCK -> {
                e.setSpan(TypefaceSpan("monospace"), s, end, F)
                e.setSpan(BackgroundColorSpan(Color.parseColor("#F8F8F8")), s, end, F)
            }
            else -> Unit
        }
    }
}