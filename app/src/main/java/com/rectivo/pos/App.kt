package com.rectivo.pos

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * One notification channel per POS notification source, so the user can
 * control sound/importance for Sales, AI Agent, Incomplete Orders and
 * WhatsApp independently from the Android system settings.
 *
 * The channel id sent by the server (data field "type") must match one of
 * these ids. Server sends: sale | ai | incomplete | whatsapp
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java)

        val channels = listOf(
            Triple(CH_SALE, "Sales", "New WooCommerce / POS sales"),
            Triple(CH_AI, "AI Agent", "New AI Agent customer messages"),
            Triple(CH_INCOMPLETE, "Incomplete Orders", "New incomplete orders"),
            Triple(CH_WHATSAPP, "WhatsApp", "New WhatsApp CRM messages"),
            Triple(CH_GENERAL, "General", "Other notifications")
        )

        channels.forEach { (id, name, desc) ->
            val ch = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            ch.description = desc
            ch.enableVibration(true)
            ch.enableLights(true)
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            mgr.createNotificationChannel(ch)
        }
    }

    companion object {
        const val CH_SALE = "rectivo_sale"
        const val CH_AI = "rectivo_ai"
        const val CH_INCOMPLETE = "rectivo_incomplete"
        const val CH_WHATSAPP = "rectivo_whatsapp"
        const val CH_GENERAL = "rectivo_general"

        /** Map the server "type" field to a channel id. */
        fun channelForType(type: String?): String = when (type) {
            "sale" -> CH_SALE
            "ai" -> CH_AI
            "incomplete" -> CH_INCOMPLETE
            "whatsapp" -> CH_WHATSAPP
            else -> CH_GENERAL
        }
    }
}
