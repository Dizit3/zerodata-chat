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
import com.zerodata.chat.BuildConfig
import com.zerodata.chat.util.Constants
import com.zerodata.chat.util.IncomingMessageNotifier
import com.zerodata.chat.util.IncomingMessageNotifierImpl
import com.zerodata.chat.util.MqttDiagReporter
import com.zerodata.chat.util.MqttDiagReporterImpl
import com.zerodata.chat.util.OkHttpDiagPoster
import com.zerodata.chat.util.SharedPrefsDiagQueueStore
import com.zerodata.chat.util.UpdateManager
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
    single<MqttDiagReporter> {
        MqttDiagReporterImpl(
            mqttUri = Constants.MQTT_SERVER_URI,
            ingestUrl = BuildConfig.DIAG_INGEST_URL,
            clientId = Constants.mqttClientId(get()),
            store = SharedPrefsDiagQueueStore(androidContext()),
            poster = OkHttpDiagPoster(),
        )
    }
    single { RealMqttManager(get(), get(), get(), get()) }
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

    single<IncomingMessageNotifier> { IncomingMessageNotifierImpl(androidContext()) }

    // Repository
    single<ChatRepository> {
        ChatRepositoryImpl(get(), get<String>(), get(), get(), get())
    }

    single { UpdateManager(androidContext()) }

    // ViewModels
    viewModel { MainViewModel(get(), androidContext(), get()) }
    viewModel { (chatId: String) -> ChatViewModel(get<ChatRepository>(), chatId) }
    viewModel { LobbyViewModel(get<MqttDiscoveryManager>(), get<String>()) }
}
