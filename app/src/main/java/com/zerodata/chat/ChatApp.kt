package com.zerodata.chat

import android.app.Application
import com.zerodata.chat.di.appModule
import com.zerodata.chat.repository.ChatRepository
import com.zerodata.chat.service.MqttKeepAliveService
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import timber.log.Timber

class ChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        startKoin {
            androidContext(this@ChatApp)
            modules(appModule)
        }
        // Warm ChatRepository (MQTT connect) then pin the process with FGS.
        GlobalContext.get().get<ChatRepository>()
        MqttKeepAliveService.start(this)
    }
}
