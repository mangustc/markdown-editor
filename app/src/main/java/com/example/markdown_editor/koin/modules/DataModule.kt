package com.example.markdown_editor.koin.modules

import androidx.room.Room
import com.example.markdown_editor.data.database.LinkPreviewDao
import com.example.markdown_editor.data.database.NoteDao
import com.example.markdown_editor.data.database.NoteDb
import com.example.markdown_editor.data.linkPreview.AndroidLinkPreviewRepository
import com.example.markdown_editor.data.project.AndroidProjectRepository
import com.example.markdown_editor.data.project.AndroidSettingsRepository
import com.example.markdown_editor.data.sync.SyncRepositoryFactory
import com.example.markdown_editor.domain.repositories.LinkPreviewRepository
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.repositories.SettingsRepository
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.single

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

val dataModule = module {
    single { create(::provideNoteDb) }
    single { create(::provideNoteDao) }
    single { create(::provideLinkPreviewDao) }

    single<AndroidProjectRepository>() bind ProjectRepository::class
    single<AndroidSettingsRepository>() bind SettingsRepository::class
    single<AndroidLinkPreviewRepository>() bind LinkPreviewRepository::class
    single<SyncRepositoryFactory>()
}