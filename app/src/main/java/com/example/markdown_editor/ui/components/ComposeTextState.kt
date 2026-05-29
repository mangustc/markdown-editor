package com.example.markdown_editor.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.UndoState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.text.TextRange
import com.example.markdown_editor.domain.models.MutableTextState
import com.example.markdown_editor.domain.models.TextSelection
import com.example.markdown_editor.domain.models.TextState
import com.example.markdown_editor.domain.models.TextUndoState

class ComposeTextUndoState(
    val undoState: UndoState,
) : TextUndoState {
    override val canUndo: Boolean
        get() = undoState.canUndo
    override val canRedo: Boolean
        get() = undoState.canRedo

    override fun undo() {
        undoState.undo()
    }

    override fun redo() {
        undoState.redo()
    }

    override fun clearHistory() {
        undoState.clearHistory()
    }
}

@OptIn(ExperimentalFoundationApi::class)
class ComposeTextState(
    val state: TextFieldState = TextFieldState(),
) : TextState {
    override val text: CharSequence
        get() = state.text

    override val selection: TextSelection
        get() = TextSelection(
            start = state.selection.start,
            end = state.selection.end,
        )

    override fun edit(block: MutableTextState.() -> Unit) {
        state.edit {
            val wrapper = ComposeMutableTextState(this)
            wrapper.block()
        }
    }

    override val undoState: TextUndoState
        get() = ComposeTextUndoState(state.undoState)

    override fun setTextAndPlaceCursorAtEnd(text: String) {
        state.setTextAndPlaceCursorAtEnd(text)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class ComposeMutableTextState(
    private val buffer: TextFieldBuffer,
) : MutableTextState {
    override val originalText: CharSequence
        get() = buffer.originalText

    override var selection: TextSelection
        get() = TextSelection(
            start = buffer.selection.start,
            end = buffer.selection.end,
        )
        set(value) {
            buffer.selection = TextRange(value.start, value.end)
        }

    override fun replace(start: Int, end: Int, text: String) {
        buffer.replace(start, end, text)
    }

    override fun insert(index: Int, text: String) {
        buffer.insert(index, text)
    }

    override fun append(text: String) {
        buffer.append(text)
    }

    override fun placeCursorAfterCharAt(index: Int) {
        buffer.placeCursorAfterCharAt(index)
    }

    override val length: Int
        get() = buffer.length
}