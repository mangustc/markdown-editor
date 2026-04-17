package com.example.markdown_editor.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, NoteEntityFts::class, LinkPreviewEntity::class],
    version = 4,
    exportSchema = false
)
abstract class NoteDb : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun linkPreviewDao(): LinkPreviewDao
}
