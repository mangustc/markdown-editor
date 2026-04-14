package com.example.markdown_editor.data.database

import androidx.room.*

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("SELECT * FROM notes WHERE uri = :uri LIMIT 1")
    suspend fun getNoteByUri(uri: String): NoteEntity?

    @Query("""
        SELECT * FROM notes 
        WHERE tags LIKE '%' || :tag || '%' 
        AND name LIKE '%' || :name || '%'
        ORDER BY lastModified DESC
    """)
    suspend fun searchMetadata(tag: String, name: String): List<NoteEntity>

    @Query("""
        SELECT m.* FROM notes m
        JOIN notesFts f ON m.rowid = f.rowid
        WHERE notesFts MATCH :query
    """)
    suspend fun searchFullText(query: String): List<NoteEntity>

    @Query("SELECT * FROM notes ORDER BY lastModified DESC")
    fun getAllNotesPaged(): androidx.paging.PagingSource<Int, NoteEntity>
}