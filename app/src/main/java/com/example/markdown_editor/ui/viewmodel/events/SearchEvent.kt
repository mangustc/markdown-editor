package com.example.markdown_editor.ui.viewmodel.events

import com.example.markdown_editor.domain.models.TextState
import com.example.markdown_editor.domain.textStateExtensions.appendName
import com.example.markdown_editor.domain.textStateExtensions.appendTag
import com.example.markdown_editor.domain.textStateExtensions.clear
import com.example.markdown_editor.domain.textStateExtensions.toggleNegation

sealed interface SearchEvent {
    data object AppendTag : SearchEvent
    data object AppendName : SearchEvent
    data object ToggleNegation : SearchEvent
    data object Clear : SearchEvent

    fun execute(state: TextState) {
        when (this) {
            is AppendTag -> {
                state.appendTag()
            }

            is AppendName -> {
                state.appendName()
            }

            is ToggleNegation -> {
                state.toggleNegation()
            }

            is Clear -> {
                state.clear()
            }
        }
    }
}