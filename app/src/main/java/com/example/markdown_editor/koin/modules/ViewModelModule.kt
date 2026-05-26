package com.example.markdown_editor.koin.modules

import com.example.markdown_editor.ui.viewmodel.AppViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val viewModelModule = module {
    viewModel<AppViewModel>()
}