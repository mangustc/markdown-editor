package com.example.markdown_editor.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        NoteEntity::class,
        NoteEntityFts::class,
        LinkPreviewEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class NoteDb : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun noteDao(): NoteDao
    abstract fun linkPreviewDao(): LinkPreviewDao
}