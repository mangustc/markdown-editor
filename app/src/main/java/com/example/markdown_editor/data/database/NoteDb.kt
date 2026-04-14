package com.example.markdown_editor.data.database

import androidx.room.*

@Database(
    entities = [NoteEntity::class, NoteEntityFts::class],
    version = 2,
    exportSchema = false
)
abstract class NoteDb : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
