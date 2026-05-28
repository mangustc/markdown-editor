package com.example.markdown_editor.koin.modules

import com.example.markdown_editor.domain.usecases.messenger.GetMessagesUseCase
import com.example.markdown_editor.domain.usecases.messenger.GetPinnedMessagesUseCase
import com.example.markdown_editor.domain.usecases.project.LoadSavedProjectUseCase
import com.example.markdown_editor.domain.usecases.project.SelectProjectUseCase
import com.example.markdown_editor.domain.usecases.project.SyncDatabaseUseCase
import com.example.markdown_editor.domain.usecases.settings.GetSettingsUseCase
import com.example.markdown_editor.domain.usecases.settings.SetSettingsUseCase
import com.example.markdown_editor.domain.usecases.sync.SyncProjectUseCase
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory

val domainModule = module {
    factory<SyncProjectUseCase>()
    factory<GetMessagesUseCase>()
    factory<GetPinnedMessagesUseCase>()
    factory<LoadSavedProjectUseCase>()
    factory<SelectProjectUseCase>()
    factory<SetSettingsUseCase>()
    factory<GetSettingsUseCase>()
    factory<SyncDatabaseUseCase>()
}
