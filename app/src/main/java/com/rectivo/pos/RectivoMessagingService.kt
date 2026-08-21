package com.rectivo.pos

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM messages and shows a native notification, even when the app is
 * in the background or killed (as long as the app is not force-stopped).
 *
 * The server sends DATA-only messages with these keys:
 *   title, body, type (sale|ai|incomplete|whatsapp), url (relative POS path)
 */
class RectivoMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Token refreshed. The WebView re-registers it on the next page load.
        // Nothing to do here; MainActivity handles registration with the session.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "Rectivo POS"
        val body = data["body"] ?: message.notification?.body ?: "You have a new notification"
        val type = data["type"]
        val path = data["url"] ?: ""

        val fullUrl = when {
            path.startsWith("http") -> path
            path.isNotEmpty() -> MainActivity.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
            else -> MainActivity.BASE_URL
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_URL, fullUrl)
        }
        val pending = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = App.channelForType(type)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.splash_logo))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pending)

        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Unique id per message so multiple notifications stack rather than replace
        mgr.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
