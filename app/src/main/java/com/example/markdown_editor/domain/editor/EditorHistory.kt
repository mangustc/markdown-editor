package com.example.markdown_editor.domain.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class EditDelta(
    val position: Int,
    val deleted: String,   // what was removed (empty on pure insert)
    val inserted: String,  // what was added  (empty on pure delete)
    val cursorBefore: Int,
    val cursorAfter: Int,
)

class EditorHistory(private val maxSize: Int = 100) {
    private val undoStack = ArrayDeque<EditDelta>()
    private val redoStack = ArrayDeque<EditDelta>()

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    fun push(old: TextFieldValue, new: TextFieldValue) {
        val delta = diff(old, new) ?: return
        undoStack.addLast(delta)
        if (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: TextFieldValue): TextFieldValue? {
        val delta = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(delta)
        return applyReverse(current, delta)
    }

    fun redo(current: TextFieldValue): TextFieldValue? {
        val delta = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(delta)
        return applyForward(current, delta)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    private fun diff(old: TextFieldValue, new: TextFieldValue): EditDelta? {
        val o = old.text
        val n = new.text
        if (o == n) return null

        var start = 0
        while (start < o.length && start < n.length && o[start] == n[start]) start++
        var endO = o.length
        var endN = n.length
        while (endO > start && endN > start && o[endO - 1] == n[endN - 1]) {
            endO--; endN--
        }

        return EditDelta(
            position = start,
            deleted = o.substring(start, endO),
            inserted = n.substring(start, endN),
            cursorBefore = old.selection.start,
            cursorAfter = new.selection.start,
        )
    }

    private fun applyReverse(current: TextFieldValue, d: EditDelta): TextFieldValue {
        val t = current.text
        val restored =
            t.substring(0, d.position) + d.deleted + t.substring(d.position + d.inserted.length)
        return TextFieldValue(restored, TextRange(d.cursorBefore))
    }

    private fun applyForward(current: TextFieldValue, d: EditDelta): TextFieldValue {
        val t = current.text
        val reapplied =
            t.substring(0, d.position) + d.inserted + t.substring(d.position + d.deleted.length)
        return TextFieldValue(reapplied, TextRange(d.cursorAfter))
    }
}