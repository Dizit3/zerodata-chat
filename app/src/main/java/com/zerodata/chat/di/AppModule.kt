package com.zerodata.chat.di

import android.content.Context
import android.provider.Settings
import com.zerodata.chat.network.MqttDiscoveryManager
import com.zerodata.chat.network.MqttMessagingManager
import com.zerodata.chat.network.MqttManager
import com.zerodata.chat.network.MqttPayloadMapper
import com.zerodata.chat.network.MqttPayloadMapperImpl
import com.zerodata.chat.network.RealMqttManager
import com.zerodata.chat.repository.ChatRepository
import com.zerodata.chat.repository.ChatRepositoryImpl
import com.zerodata.chat.viewmodel.ChatViewModel
import com.zerodata.chat.viewmodel.LobbyViewModel
import com.zerodata.chat.viewmodel.MainViewModel
import com.zerodata.chat.database.AppDatabase
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Generate a stable ID based on device ANDROID_ID
    single { 
        val context: Context = get()
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        "@" + (androidId?.take(8) ?: "user_" + (1000..9999).random())
    }

    // Mappers
    // Network
    single<MqttPayloadMapper> { MqttPayloadMapperImpl() }
    single { RealMqttManager(get(), get(), get()) }
    single<MqttManager> { get<RealMqttManager>() }
    single<MqttMessagingManager> { get<RealMqttManager>() }
    single<MqttDiscoveryManager> { get<RealMqttManager>() }

    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "zerodata_chat.db"
        ).build()
    }
    single { get<AppDatabase>().chatDao() }
    single { get<AppDatabase>().messageDao() }

    // Repository
    single<ChatRepository> { ChatRepositoryImpl(get(), get<String>(), get(), get()) }

    // ViewModels
    viewModel { MainViewModel(get(), androidContext()) }
    viewModel { (chatId: String) -> ChatViewModel(get<ChatRepository>(), chatId) }
    viewModel { LobbyViewModel(get<MqttDiscoveryManager>(), get<String>()) }
}
