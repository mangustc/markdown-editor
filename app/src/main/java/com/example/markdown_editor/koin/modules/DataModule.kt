package com.example.markdown_editor.koin.modules

import android.content.Context
import androidx.room.Room
import com.example.markdown_editor.data.database.LinkPreviewDao
import com.example.markdown_editor.data.database.NoteDao
import com.example.markdown_editor.data.database.NoteDb
import com.example.markdown_editor.data.database.ProjectDao
import com.example.markdown_editor.data.linkPreview.CommonLinkPreviewRepository
import com.example.markdown_editor.data.project.AndroidPlatformPathHandler
import com.example.markdown_editor.data.project.AndroidProjectRepository
import com.example.markdown_editor.data.project.CommonSettingsRepository
import com.example.markdown_editor.data.sync.SyncRepositoryFactory
import com.example.markdown_editor.domain.repositories.LinkPreviewRepository
import com.example.markdown_editor.domain.repositories.PlatformPathHandler
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.repositories.SettingsRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.single

fun provideNoteDb(context: Context): NoteDb {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("database-notes")
    val databaseBuilder = Room.databaseBuilder<NoteDb>(
        context = appContext,
        name = dbFile.absolutePath,
    )
    return databaseBuilder
        .fallbackToDestructiveMigration(true)
        .build()
}

fun provideProjectDao(db: NoteDb): ProjectDao = db.projectDao()

fun provideNoteDao(db: NoteDb): NoteDao = db.noteDao()

fun provideLinkPreviewDao(db: NoteDb): LinkPreviewDao = db.linkPreviewDao()

fun provideHttpClient(): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            },
        )
    }
}

fun provideFactory(context: Context): Settings.Factory = SharedPreferencesSettings.Factory(context)

fun providePlatformPathHandler(context: Context): PlatformPathHandler =
    AndroidPlatformPathHandler(context)

val dataModule = module {
    single { create(::provideNoteDb) }
    single { create(::provideProjectDao) }
    single { create(::provideNoteDao) }
    single { create(::provideLinkPreviewDao) }
    single { create(::provideHttpClient) }
    single { create(::provideFactory) }
    single { create(::providePlatformPathHandler) }

    single<AndroidProjectRepository>() bind ProjectRepository::class
    single<CommonSettingsRepository>() bind SettingsRepository::class
    single<CommonLinkPreviewRepository>() bind LinkPreviewRepository::class
    single<SyncRepositoryFactory>()
}