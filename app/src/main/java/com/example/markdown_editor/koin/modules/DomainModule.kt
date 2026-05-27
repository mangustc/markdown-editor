package com.example.markdown_editor.koin.modules

import com.example.markdown_editor.domain.usecases.messenger.GetMessagesUseCase
import com.example.markdown_editor.domain.usecases.sync.SyncProjectUseCase
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory

val domainModule = module {
    factory<SyncProjectUseCase>()
    factory<GetMessagesUseCase>()
}
