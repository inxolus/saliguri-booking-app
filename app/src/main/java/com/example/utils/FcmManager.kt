package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.network.RetrofitClient
import com.example.network.SyncRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class FcmNotification(val title: String, val body: String, val timestamp: Long)

class FcmManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun registerDeviceToCloud(deviceId: String, token: String? = null) {
        try {
            val fcmToken = token ?: FirebaseMessaging.getInstance().token.await()
            
            // Simpan token lokal
            prefs.edit().putString(KEY_TOKEN, fcmToken).apply()
            
            // Kirim ke Apps Script (endpoint registerDevice)
            val response = RetrofitClient.api.postData(
                SyncRequest(
                    action = "registerDevice",
                    sheet = "Devices",
                    data = mapOf(
                        "fcmToken" to fcmToken,
                        "deviceId" to deviceId,
                        "deviceName" to android.os.Build.MODEL,
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("FCM", "Device registered to cloud")
                
                // SUBSCRIBE KE TOPIC - INI PENTING!
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_SALIGURI).await()
                Log.d("FCM", "Subscribed to topic $TOPIC_SALIGURI")
            }
        } catch (e: Exception) {
            Log.e("FCM", "Error: ${e.message}")
        }
    }

    fun getSavedToken(): String? = prefs.getString(KEY_TOKEN, null)

    companion object {
        private const val PREFS_NAME = "fcm_prefs"
        private const val KEY_TOKEN = "fcm_token"
        private const val KEY_NOTIFICATIONS = "fcm_notifications"
        const val TOPIC_SALIGURI = "saliguri_bookings"

        private val _newNotificationFlow = MutableSharedFlow<FcmNotification>(extraBufferCapacity = 10)
        val newNotificationFlow = _newNotificationFlow.asSharedFlow()

        fun saveToken(context: Context, token: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }

        fun getToken(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_TOKEN, "") ?: ""
        }

        fun saveNotification(context: Context, title: String, body: String, timestamp: Long = System.currentTimeMillis()) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val list = getNotifications(context).toMutableList()
            val newItem = FcmNotification(title, body, timestamp)
            list.add(0, newItem) // Add to top

            // Limit to last 50 notifications
            val limitedList = if (list.size > 50) list.subList(0, 50) else list

            val jsonArray = JSONArray()
            for (item in limitedList) {
                val jsonObject = JSONObject().apply {
                    put("title", item.title)
                    put("body", item.body)
                    put("timestamp", item.timestamp)
                }
                jsonArray.put(jsonObject)
            }

            prefs.edit().putString(KEY_NOTIFICATIONS, jsonArray.toString()).apply()
            
            // Trigger flow
            _newNotificationFlow.tryEmit(newItem)
        }

        fun getNotifications(context: Context): List<FcmNotification> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
            val list = mutableListOf<FcmNotification>()
            try {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        FcmNotification(
                            title = obj.optString("title", ""),
                            body = obj.optString("body", ""),
                            timestamp = obj.optLong("timestamp", 0L)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }

        fun clearNotifications(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_NOTIFICATIONS).apply()
        }
    }
}
