package com.example.markdown_editor.domain.models

interface TextState {
    val text: CharSequence
    val selection: TextSelection
    val undoState: TextUndoState

    fun edit(block: MutableTextState.() -> Unit)
    fun setTextAndPlaceCursorAtEnd(text: String)
}

data class TextSelection(
    val start: Int,
    val end: Int,
) {
    val collapsed: Boolean get() = start == end
    val min: Int get() = if (start < end) start else end
    val max: Int get() = if (start > end) start else end
    val length: Int get() = max - min
}

interface TextUndoState {
    val canUndo: Boolean
    val canRedo: Boolean

    fun undo()
    fun redo()
    fun clearHistory()
}


interface MutableTextState {
    val originalText: CharSequence
    var selection: TextSelection
    val length: Int

    fun replace(start: Int, end: Int, text: String)
    fun insert(index: Int, text: String)
    fun append(text: String)
    fun placeCursorAfterCharAt(index: Int)
}

