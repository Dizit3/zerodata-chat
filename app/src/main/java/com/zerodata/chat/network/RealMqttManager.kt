package com.zerodata.chat.network

import android.content.Context
import com.zerodata.chat.model.Message
import com.zerodata.chat.model.LobbyPresence
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.zerodata.chat.util.Constants
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber

class RealMqttManager(
    private val context: Context,
    private val userId: String,
    private val payloadMapper: MqttPayloadMapper = MqttPayloadMapperImpl()
) : MqttManager {
    
    private val serverUri = Constants.MQTT_SERVER_URI
    private val clientId = Constants.mqttClientId(userId)
    private val mqttClient = MqttAndroidClient(context, serverUri, clientId)

    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus = _connectionStatus.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    private val _inbound = MutableSharedFlow<MqttInbound>(extraBufferCapacity = 64)

    override suspend fun connect() {
        if (mqttClient.isConnected) {
            _connectionStatus.value = true
            return
        }
        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = false
            keepAliveInterval = Constants.MQTT_KEEP_ALIVE_SECONDS
            connectionTimeout = Constants.MQTT_CONNECTION_TIMEOUT_SECONDS
            if (serverUri.startsWith("ssl://", ignoreCase = true)) {
                socketFactory = javax.net.ssl.SSLSocketFactory.getDefault()
            }
        }

        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Connected successfully")
                    _connectionStatus.value = true
                    subscribeToTopics()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception, "Connection failed")
                    _connectionStatus.value = false
                }
            })

            mqttClient.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    if (reconnect) {
                        Timber.d("Reconnected to $serverURI")
                        _connectionStatus.value = true
                        subscribeToTopics()
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    Timber.e(cause, "Connection lost")
                    _connectionStatus.value = false
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Timber.d("Message arrived on $topic")
                    message?.let {
                        val payload = String(it.payload)
                        if (topic == Constants.TOPIC_LOBBY) {
                            handleLobbyMessage(payload)
                        } else {
                            payloadMapper.parseInbound(payload)?.let { inbound ->
                                _inbound.tryEmit(inbound)
                                if (inbound is MqttInbound.ChatMessage) {
                                    _incomingMessages.tryEmit(inbound.message)
                                }
                            }
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })
        } catch (e: Exception) {
            Timber.e(e, "Error during connect")
        }
    }

    private fun subscribeToTopics() {
        val topic = "${Constants.TOPIC_PREFIX}/$userId/${Constants.TOPIC_INBOX_SUFFIX}"
        mqttClient.subscribe(topic, Constants.QOS_LEAST_ONCE, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Timber.d("Subscribed to $topic")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Timber.e(exception, "Subscription failed")
            }
        })
    }

    override suspend fun disconnect() {
        if (mqttClient.isConnected) {
            try {
                mqttClient.disconnect()
            } catch (e: Exception) {
                Timber.e(e, "Error during disconnect")
            }
        }
    }

    override suspend fun sendMessage(message: Message) {
        if (!mqttClient.isConnected) {
            throw IllegalStateException("MQTT not connected")
        }
        val topic = "${Constants.TOPIC_PREFIX}/${message.receiverId}/${Constants.TOPIC_INBOX_SUFFIX}"
        val payload = payloadMapper.toMessageJson(message)
        val mqttMessage = MqttMessage(payload.toByteArray()).apply {
            qos = Constants.QOS_LEAST_ONCE
        }
        try {
            mqttClient.publish(topic, mqttMessage)
            Timber.d("Message published to $topic")
        } catch (e: Exception) {
            Timber.e(e, "Failed to publish message")
            throw e
        }
    }

    override fun observeMessages(): Flow<Message> = _incomingMessages.asSharedFlow()

    override fun observeInbound(): Flow<MqttInbound> = _inbound.asSharedFlow()

    override suspend fun sendReceipt(recipientId: String, kind: ReceiptKind, messageId: String) {
        if (!mqttClient.isConnected) return
        val topic = "${Constants.TOPIC_PREFIX}/$recipientId/${Constants.TOPIC_INBOX_SUFFIX}"
        val mqttMessage = MqttMessage(payloadMapper.toReceiptJson(kind, messageId).toByteArray()).apply {
            qos = Constants.QOS_LEAST_ONCE
        }
        try {
            mqttClient.publish(topic, mqttMessage)
        } catch (e: Exception) {
            Timber.e(e, "Failed to publish receipt")
        }
    }

    private val _lobbyPresences = MutableSharedFlow<LobbyPresence>(extraBufferCapacity = 64)
    private var lobbyJob: kotlinx.coroutines.Job? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun joinLobby() {
        if (lobbyJob != null) return
        
        mqttClient.subscribe(Constants.TOPIC_LOBBY, Constants.QOS_AT_MOST_ONCE)

        lobbyJob = scope.launch {
            while (isActive) {
                sendPresence()
                delay(Constants.LOBBY_HEARTBEAT_MS)
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
            Timber.e(e, "Error unsubscribing from lobby")
        }
    }

    override fun observeLobby(): Flow<LobbyPresence> = _lobbyPresences.asSharedFlow()

    private fun sendPresence() {
        val presence = com.zerodata.chat.model.LobbyPresence(userId)
        val payload = payloadMapper.toLobbyJson(presence)
        val mqttMessage = MqttMessage(payload.toByteArray()).apply {
            qos = Constants.QOS_AT_MOST_ONCE
        }
        
        try {
            if (mqttClient.isConnected) {
                mqttClient.publish(Constants.TOPIC_LOBBY, mqttMessage)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to publish presence")
        }
    }

    // В коллбэке messageArrived нужно добавить обработку топика лобби
    private fun handleLobbyMessage(payload: String) {
        try {
            payloadMapper.fromLobbyJson(payload)?.let { presence ->
                _lobbyPresences.tryEmit(presence)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode lobby presence")
        }
    }
}
