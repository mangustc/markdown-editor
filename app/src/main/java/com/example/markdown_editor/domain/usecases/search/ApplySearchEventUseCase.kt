package com.example.markdown_editor.domain.usecases.search

import com.example.markdown_editor.domain.models.TextState
import com.example.markdown_editor.domain.usecases.UseCase

data class ApplySearchEventInput(
    val state: TextState,
    val event: SearchEvent,
)

class ApplySearchEventUseCase : UseCase<ApplySearchEventInput, Unit> {
    override suspend fun invoke(input: ApplySearchEventInput) {
        val event = input.event
        val state = input.state
        when (event) {
            SearchEvent.AppendTag -> {
                val q = state.text
                val prefix = if (q.isEmpty() || q.last().isWhitespace()) "" else " "
                state.edit {
                    append(prefix + "tag:\"\"")
                    placeCursorAfterCharAt(length - 2)
                }
            }

            SearchEvent.AppendName -> {
                val q = state.text
                val prefix = if (q.isEmpty() || q.last().isWhitespace()) "" else " "
                state.edit {
                    append(prefix + "name:\"\"")
                    placeCursorAfterCharAt(length - 2)
                }

            }

            SearchEvent.Clear -> {
                state.setTextAndPlaceCursorAtEnd("")
            }

            SearchEvent.ToggleNegation -> {
                val cursor = state.selection.start
                if (cursor < 0) return

                val textStr = state.text.toString()
                state.edit {
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
        }
    }
}
