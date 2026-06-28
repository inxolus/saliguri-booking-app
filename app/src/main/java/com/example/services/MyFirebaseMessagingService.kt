package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.utils.FcmManager
import com.example.saliguri.repository.CloudSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Data message dari Apps Script
        val data = message.data
        if (data.isNotEmpty() && data["type"] == "new_booking") {
            val guestName = data["guest_name"] ?: "Tamu Baru"
            val roomNumbers = data["room_numbers"] ?: "-"
            val checkIn = data["check_in"] ?: "-"
            val reservationId = data["reservation_id"] ?: ""
            
            // 1. Tampilkan notifikasi
            showBookingNotification(
                title = "🎉 Pesanan Baru!",
                message = "$guestName memesan kamar $roomNumbers (Check-in: $checkIn)"
            )
            
            // 2. Trigger sync segera (jangan tunggu 30 detik)
            val context = this
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = CloudSyncRepository(context)
                    val result = repo.performFullSync()
                    if (result.downloaded > 0) {
                        context.sendBroadcast(Intent(SyncService.ACTION_DATA_UPDATED))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // 3. Simpan flag ada booking baru (untuk badge di Dashboard)
            getSharedPreferences("booking_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("has_new_booking", true)
                .putString("last_booking_id", reservationId)
                .apply()
        } else {
            val title = message.notification?.title ?: data["title"] ?: "Saliguri Update"
            val body = message.notification?.body ?: data["body"] ?: "You have a new notification."
            
            showBookingNotification(title, body)
            FcmManager.saveNotification(this, title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Simpan token, nanti sync ke cloud saat app dibuka
        getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .putBoolean("token_synced", false)
            .apply()
    }

    private fun showBookingNotification(title: String, message: String) {
        val channelId = "saliguri_booking_channel_v2"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/" + com.example.R.raw.notification_reservasi)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()
                
            val channel = NotificationChannel(
                channelId,
                "Pesanan Booking",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat ada booking baru masuk"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "bookings")
            putExtra("highlight_new", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
