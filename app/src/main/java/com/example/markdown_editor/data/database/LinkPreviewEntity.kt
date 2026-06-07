package com.example.markdown_editor.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "link_previews")
data class LinkPreviewEntity(
    @PrimaryKey val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val fetchedAt: Long = Clock.System.now().toEpochMilliseconds(),
)