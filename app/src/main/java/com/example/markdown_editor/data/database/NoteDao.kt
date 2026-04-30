package com.example.markdown_editor.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("SELECT * FROM notes WHERE uri = :uri LIMIT 1")
    suspend fun getNoteByUri(uri: String): NoteEntity?

    @RawQuery
    suspend fun searchNotes(query: SupportSQLiteQuery): List<NoteEntity>

    @RawQuery(observedEntities = [NoteEntity::class, NoteEntityFts::class])
    fun searchNotesPaged(query: SupportSQLiteQuery): PagingSource<Int, NoteEntity>
}