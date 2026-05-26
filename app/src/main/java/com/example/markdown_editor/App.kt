package com.example.markdown_editor

import android.app.Application
import com.example.markdown_editor.koin.modules.repositoryModule
import com.example.markdown_editor.koin.modules.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(repositoryModule, viewModelModule)
        }
    }
}