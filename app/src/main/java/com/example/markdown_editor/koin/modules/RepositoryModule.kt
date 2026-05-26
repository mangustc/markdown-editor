package com.example.markdown_editor.koin.modules

import androidx.room.Room
import com.example.markdown_editor.data.database.LinkPreviewDao
import com.example.markdown_editor.data.database.NoteDao
import com.example.markdown_editor.data.database.NoteDb
import com.example.markdown_editor.data.repository.LinkPreviewRepository
import com.example.markdown_editor.data.repository.LinkPreviewRepositoryImpl
import com.example.markdown_editor.data.repository.NoteRepository
import com.example.markdown_editor.data.repository.NoteRepositoryImpl
import com.example.markdown_editor.data.repository.ProjectRepository
import com.example.markdown_editor.data.repository.ProjectRepositoryImpl
import com.example.markdown_editor.data.repository.SettingsRepository
import com.example.markdown_editor.data.repository.SettingsRepositoryImpl
import com.example.markdown_editor.data.sync.SyncRepository
import com.example.markdown_editor.data.sync.SyncRepositoryImpl
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.single

// Builder functions readable by compiler plugin
fun provideNoteDb(context: android.content.Context): NoteDb {
    return Room.databaseBuilder(
        context,
        NoteDb::class.java, "database-notes",
    )
        .fallbackToDestructiveMigration(true)
        .build()
}

fun provideNoteDao(db: NoteDb): NoteDao = db.noteDao()

fun provideLinkPreviewDao(db: NoteDb): LinkPreviewDao = db.linkPreviewDao()

val repositoryModule = module {
    single { create(::provideNoteDb) }
    single { create(::provideNoteDao) }
    single { create(::provideLinkPreviewDao) }

    single<ProjectRepositoryImpl>() bind ProjectRepository::class
    single<SettingsRepositoryImpl>() bind SettingsRepository::class
    single<NoteRepositoryImpl>() bind NoteRepository::class
    single<LinkPreviewRepositoryImpl>() bind LinkPreviewRepository::class
    single<SyncRepositoryImpl>() bind SyncRepository::class
}