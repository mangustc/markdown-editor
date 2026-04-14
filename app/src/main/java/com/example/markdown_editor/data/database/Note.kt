package com.example.markdown_editor.data.database

import androidx.room.*

@Entity(
    tableName = "notes",
    indices = [Index(value = ["uri"], unique = true)],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val name: String,
    val lastModified: Long,
    val createdAt: Long? = null,
    val tags: String,
    val body: String,
)

@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "notesFts")
data class NoteEntityFts(
    val id: Long,
    val name: String,
    val body: String,
)