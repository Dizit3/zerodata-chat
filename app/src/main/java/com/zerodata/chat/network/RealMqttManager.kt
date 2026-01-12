package com.zerodata.chat.network

import android.content.Context
import android.util.Log
import com.zerodata.chat.model.Message
import com.zerodata.chat.model.MessageStatus
import com.zerodata.chat.model.LobbyPresence
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.zerodata.chat.util.Constants
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*

class RealMqttManager(
    private val context: Context,
    private val userId: String,
    private val payloadMapper: MqttPayloadMapper = MqttPayloadMapperImpl()
) : MqttManager {
    
    private val serverUri = Constants.MQTT_SERVER_URI
    private val clientId = "zerodata_${userId}_${UUID.randomUUID().toString().take(5)}"
    private val mqttClient = MqttAndroidClient(context, serverUri, clientId)

    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus = _connectionStatus.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)

    override suspend fun connect() {
        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = false
        }

        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(Constants.TAG_MQTT, "Connected successfully")
                    _connectionStatus.value = true
                    subscribeToTopics()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(Constants.TAG_MQTT, "Connection failed", exception)
                    _connectionStatus.value = false
                }
            })

            mqttClient.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    if (reconnect) {
                        Log.d(Constants.TAG_MQTT, "Reconnected to $serverURI")
                        _connectionStatus.value = true
                        subscribeToTopics()
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.e(Constants.TAG_MQTT, "Connection lost", cause)
                    _connectionStatus.value = false
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d(Constants.TAG_MQTT, "Message arrived on $topic")
                    message?.let {
                        val payload = String(it.payload)
                        if (topic == Constants.TOPIC_LOBBY) {
                            handleLobbyMessage(payload)
                        } else {
                            payloadMapper.fromMessageJson(payload)?.let { msg ->
                                // Убеждаемся, что chatId проставлен корректно для группировки
                                val effectiveMsg = if (msg.chatId.isEmpty()) msg.copy(chatId = msg.senderId) else msg
                                _incomingMessages.tryEmit(effectiveMsg)
                            }
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })
        } catch (e: Exception) {
            Log.e(Constants.TAG_MQTT, "Error during connect", e)
        }
    }

    private fun subscribeToTopics() {
        val topic = "${Constants.TOPIC_PREFIX}/$userId/${Constants.TOPIC_INBOX_SUFFIX}"
        mqttClient.subscribe(topic, Constants.QOS_LEAST_ONCE, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(Constants.TAG_MQTT, "Subscribed to $topic")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e(Constants.TAG_MQTT, "Subscription failed", exception)
            }
        })
    }

    override suspend fun disconnect() {
        if (mqttClient.isConnected) {
            try {
                mqttClient.disconnect()
            } catch (e: Exception) {
                Log.e(Constants.TAG_MQTT, "Error during disconnect", e)
            }
        }
    }

    override suspend fun sendMessage(message: Message) {
        val topic = "${Constants.TOPIC_PREFIX}/${message.receiverId}/${Constants.TOPIC_INBOX_SUFFIX}"
        val payload = payloadMapper.toMessageJson(message)
        val mqttMessage = MqttMessage(payload.toByteArray()).apply {
            qos = Constants.QOS_LEAST_ONCE
        }
        
        try {
            mqttClient.publish(topic, mqttMessage)
            Log.d(Constants.TAG_MQTT, "Message published to $topic")
        } catch (e: Exception) {
            Log.e(Constants.TAG_MQTT, "Failed to publish message", e)
        }
    }

    override fun observeMessages(): Flow<Message> = _incomingMessages.asSharedFlow()

    private val _lobbyPresences = MutableSharedFlow<LobbyPresence>(extraBufferCapacity = 64)
    private var lobbyJob: kotlinx.coroutines.Job? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun joinLobby() {
        if (lobbyJob != null) return
        
        mqttClient.subscribe(Constants.TOPIC_LOBBY, Constants.QOS_LEAST_ONCE)
        
        lobbyJob = scope.launch {
            while (kotlinx.coroutines.isActive) {
                sendPresence()
                kotlinx.coroutines.delay(5000) // Отправляем присутствие каждые 5 сек
            }
        }
    }

    override fun leaveLobby() {
        lobbyJob?.cancel()
        lobbyJob = null
        try {
            if (mqttClient.isConnected) {
                mqttClient.unsubscribe(Constants.TOPIC_LOBBY)
            }
        } catch (e: Exception) {
            Log.e(Constants.TAG_MQTT, "Error unsubscribing from lobby", e)
        }
    }

    override fun observeLobby(): Flow<LobbyPresence> = _lobbyPresences.asSharedFlow()

    private fun sendPresence() {
        val presence = com.zerodata.chat.model.LobbyPresence(userId)
        val payload = payloadMapper.toLobbyJson(presence)
        val mqttMessage = MqttMessage(payload.toByteArray()).apply {
            qos = 0 // Для присутствия достаточно QOS 0
        }
        
        try {
            if (mqttClient.isConnected) {
                mqttClient.publish(Constants.TOPIC_LOBBY, mqttMessage)
            }
        } catch (e: Exception) {
            Log.e(Constants.TAG_MQTT, "Failed to publish presence", e)
        }
    }

    // В коллбэке messageArrived нужно добавить обработку топика лобби
    private fun handleLobbyMessage(payload: String) {
        try {
            payloadMapper.fromLobbyJson(payload)?.let { presence ->
                _lobbyPresences.tryEmit(presence)
            }
        } catch (e: Exception) {
            Log.e(Constants.TAG_MQTT, "Failed to decode lobby presence", e)
        }
    }
}
