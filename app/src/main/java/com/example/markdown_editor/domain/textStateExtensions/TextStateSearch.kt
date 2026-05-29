package com.example.markdown_editor.domain.textStateExtensions

import com.example.markdown_editor.domain.models.TextState

fun TextState.appendTag() {
    val q = text
    val prefix = if (q.isEmpty() || q.last().isWhitespace()) "" else " "
    edit {
        append(prefix + "tag:\"\"")
        placeCursorAfterCharAt(length - 2)
    }
}

fun TextState.appendName() {
    val q = text
    val prefix = if (q.isEmpty() || q.last().isWhitespace()) "" else " "
    edit {
        append(prefix + "name:\"\"")
        placeCursorAfterCharAt(length - 2)
    }
}

fun TextState.toggleNegation() {
    val cursor = selection.start
    if (cursor < 0) return

    val textStr = text.toString()
    edit {
        val tokenRegex = Regex("""(?:[^\s"]|"[^"]*")+""")
        val match = tokenRegex.findAll(textStr).find { matchResult ->
            cursor in matchResult.range.first..(matchResult.range.last + 1)
        }

        if (match != null) {
            val start = match.range.first
            val end = match.range.last + 1
            val token = match.value
            if (token.startsWith("-")) {
                replace(start, end, token.drop(1))
            } else {
                replace(start, end, "-$token")
            }
        }
    }
}

fun TextState.clear() {
    setTextAndPlaceCursorAtEnd("")
}
