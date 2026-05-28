package com.example.markdown_editor.data.database

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

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

@Fts4(
    contentEntity = NoteEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
)
@Entity(tableName = "notesFts")
data class NoteEntityFts(
    val id: Long,
    val name: String,
    val body: String,
)