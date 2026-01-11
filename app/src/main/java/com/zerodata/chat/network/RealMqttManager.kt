package com.zerodata.chat.network

import android.content.Context
import android.util.Log
import com.zerodata.chat.model.Message
import com.zerodata.chat.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import java.util.*

class RealMqttManager(private val context: Context, private val userId: String) : MqttManager {
    private val serverUri = "tcp://broker.emqx.io:1883"
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
                    Log.d("MQTT", "Connected successfully")
                    _connectionStatus.value = true
                    subscribeToTopics()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Connection failed", exception)
                    _connectionStatus.value = false
                }
            })

            mqttClient.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    if (reconnect) {
                        Log.d("MQTT", "Reconnected to $serverURI")
                        _connectionStatus.value = true
                        subscribeToTopics()
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "Connection lost", cause)
                    _connectionStatus.value = false
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("MQTT", "Message arrived on $topic")
                    message?.let {
                        val payload = String(it.payload)
                        parseMessage(payload)?.let { msg ->
                            _incomingMessages.tryEmit(msg)
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })
        } catch (e: Exception) {
            Log.e("MQTT", "Error during connect", e)
        }
    }

    private fun subscribeToTopics() {
        // Подписываемся на входящие сообщения для этого пользователя
        val topic = "zerodata/users/$userId/inbox"
        mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "Subscribed to $topic")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "Subscription failed", exception)
            }
        })
    }

    override suspend fun disconnect() {
        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }

    override suspend fun sendMessage(message: Message) {
        val topic = "zerodata/users/${message.receiverId}/inbox"
        val json = JSONObject().apply {
            put("id", message.id)
            put("senderId", message.senderId)
            put("receiverId", message.receiverId)
            put("text", message.text)
            put("timestamp", message.timestamp)
        }
        val mqttMessage = MqttMessage(json.toString().toByteArray()).apply {
            qos = 1
        }
        
        try {
            mqttClient.publish(topic, mqttMessage)
            Log.d("MQTT", "Message published to $topic")
        } catch (e: Exception) {
            Log.e("MQTT", "Failed to publish message", e)
        }
    }

    override fun observeMessages(): Flow<Message> = _incomingMessages.asSharedFlow()

    private fun parseMessage(payload: String): Message? {
        return try {
            val json = JSONObject(payload)
            Message(
                id = json.getString("id"),
                senderId = json.getString("senderId"),
                receiverId = json.getString("receiverId"),
                text = json.getString("text"),
                timestamp = json.getLong("timestamp"),
                status = MessageStatus.SENT,
                chatId = json.getString("senderId") // Для теста: чат ID = отправитель
            )
        } catch (e: Exception) {
            Log.e("MQTT", "Failed to parse message", e)
            null
        }
    }
}
