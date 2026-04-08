package com.example.markdown_editor.ui.editor

sealed class EditorEvent {
    // cursorOffset: how many chars from the start of the inserted syntax
    // the cursor should land — e.g. "****" with offset 2 puts cursor between the stars
    data class InsertSyntax(val syntax: String, val cursorOffset: Int) : EditorEvent()
}