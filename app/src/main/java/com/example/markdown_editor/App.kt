package com.example.markdown_editor

import android.app.Application
import com.example.markdown_editor.koin.modules.dataModule
import com.example.markdown_editor.koin.modules.domainModule
import com.example.markdown_editor.koin.modules.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(dataModule, domainModule, viewModelModule)
        }
    }
}