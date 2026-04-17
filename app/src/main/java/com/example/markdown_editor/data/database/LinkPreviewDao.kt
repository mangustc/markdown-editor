package com.example.markdown_editor.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LinkPreviewDao {
    @Query("SELECT * FROM link_previews WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): LinkPreviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LinkPreviewEntity)
}