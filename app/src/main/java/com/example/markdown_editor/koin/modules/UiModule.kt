package com.example.markdown_editor.koin.modules

import com.example.markdown_editor.ui.util.AndroidDateFormatter
import com.example.markdown_editor.ui.util.DateFormatter
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val viewModelModule = module {
    single<AndroidDateFormatter>() bind DateFormatter::class
    viewModel<AppViewModel>()
}