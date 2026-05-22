package com.example.markdown_editor.domain.navigation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd

sealed class SearchEvent {
    object AppendTag : SearchEvent()
    object AppendName : SearchEvent()
    object ToggleNegation : SearchEvent()
    object Clear : SearchEvent()

    fun execute(searchState: TextFieldState) {
        when (this) {
            is AppendTag -> {
                val q = searchState.text
                val prefix = if (q.isEmpty() || q.last().isWhitespace()) "" else " "
                searchState.edit {
                    append(prefix + "tag:\"\"")
                    placeCursorAfterCharAt(length - 2)
                }
            }

            is AppendName -> {
                val q = searchState.text
                val prefix = if (q.isEmpty() || q.last().isWhitespace()) "" else " "
                searchState.edit {
                    append(prefix + "name:\"\"")
                    placeCursorAfterCharAt(length - 2)
                }
            }

            is ToggleNegation -> {
                searchState.edit {
                    val cursor = selection.start
                    if (cursor < 0) return@edit
                    val textStr = toString()

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

            is Clear -> {
                searchState.setTextAndPlaceCursorAtEnd("")
            }
        }
    }
}