package com.example.markdown_editor.domain.usecases.editor

import com.example.markdown_editor.domain.models.MutableTextState
import com.example.markdown_editor.domain.models.TextSelection
import com.example.markdown_editor.domain.models.TextState

private fun escapeMarkdownLabel(label: String): String {
    return buildString(label.length * 2) {
        for (char in label) {
            if (char == '[' || char == ']' || char == '\\') {
                append('\\')
            }
            append(char)
        }
    }
}

fun MutableTextState.insertWithOffset(text: String, offset: Int) {
    if (selection.collapsed) {
        val start = selection.start
        replace(start, start, text)
        placeCursorAfterCharAt(start + offset - 1)
    } else {
        val selStart = selection.min
        val selEnd = selection.max
        val selLength = selection.length
        val prefix = text.substring(0, offset)
        val suffix = text.substring(offset)
        insert(selEnd, suffix)
        insert(selStart, prefix)
        selection = TextSelection(
            selStart + prefix.length,
            selStart + prefix.length + selLength,
        )
    }
}

fun MutableTextState.bold() {
    insertWithOffset("****", 2)
}

fun MutableTextState.italic() {
    insertWithOffset("**", 1)
}

fun MutableTextState.code() {
    insertWithOffset("``", 1)
}

fun TextState.insertLink(label: String, payload: String, isImage: Boolean = false) {
    val escapedLabel = escapeMarkdownLabel(label)
    val prefix = if (isImage) "!" else ""
    edit {
        insert(selection.max, "$prefix[$escapedLabel](<$payload>)")
    }
}
