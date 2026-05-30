package com.example.markdown_editor.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("SELECT id FROM projects WHERE rootPath = :rootPath LIMIT 1")
    suspend fun getProjectId(rootPath: String): Long?
}