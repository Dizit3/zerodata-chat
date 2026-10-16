package com.zerodata.chat.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zerodata.chat.MainActivity
import com.zerodata.chat.R

interface IncomingMessageNotifier {
    fun notifyIncoming(chatId: String, senderId: String, text: String)
    fun cancel(chatId: String)

    companion object {
        const val EXTRA_CHAT_ID = "com.zerodata.chat.OPEN_CHAT_ID"
        const val CHANNEL_ID = "zerodata_incoming"
    }
}

class NoOpIncomingMessageNotifier : IncomingMessageNotifier {
    override fun notifyIncoming(chatId: String, senderId: String, text: String) = Unit
    override fun cancel(chatId: String) = Unit
}

class IncomingMessageNotifierImpl(
    private val context: Context,
) : IncomingMessageNotifier {

    override fun notifyIncoming(chatId: String, senderId: String, text: String) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(IncomingMessageNotifier.EXTRA_CHAT_ID, chatId)
        }
        val requestCode = IncomingNotificationPolicy.notificationId(chatId)
        val pending = PendingIntent.getActivity(
            context,
            requestCode,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, IncomingMessageNotifier.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_placeholder)
            .setContentTitle(senderId)
            .setContentText(IncomingNotificationPolicy.previewText(text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(IncomingNotificationPolicy.previewText(text, 400)),
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        try {
            manager.notify(requestCode, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied on API 33+
        }
    }

    override fun cancel(chatId: String) {
        NotificationManagerCompat.from(context)
            .cancel(IncomingNotificationPolicy.notificationId(chatId))
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            IncomingMessageNotifier.CHANNEL_ID,
            context.getString(R.string.incoming_message_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.incoming_message_channel)
        }
        manager.createNotificationChannel(channel)
    }
}
