package com.zerodata.chat.di

import com.zerodata.chat.network.MessageMapper
import com.zerodata.chat.network.MessageMapperImpl
import com.zerodata.chat.network.MqttManager
import com.zerodata.chat.network.RealMqttManager
import com.zerodata.chat.repository.ChatRepository
import com.zerodata.chat.repository.ChatRepositoryImpl
import com.zerodata.chat.viewmodel.ChatViewModel
import com.zerodata.chat.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Generate an ID for this session
    single { "user_" + (1000..9999).random() }

    // Mappers
    single<MessageMapper> { MessageMapperImpl() }

    // Network
    single<MqttManager> { RealMqttManager(get(), get(), get()) }

    // Repository
    single<ChatRepository> { ChatRepositoryImpl(get(), get()) }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { (chatId: String) -> ChatViewModel(get(), chatId) }
}
