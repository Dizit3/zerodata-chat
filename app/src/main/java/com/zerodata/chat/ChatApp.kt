package com.zerodata.chat

import android.app.Application
import com.zerodata.chat.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@ChatApp)
            modules(appModule)
        }
    }
}
