package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.data.local.AppDatabase
import com.example.saliguri.repository.CloudSyncRepository
import kotlinx.coroutines.*

class SyncService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSyncLoop()
            ACTION_STOP -> stopService()
            ACTION_SYNC_NOW -> scope.launch { performSync() }
        }
        return START_STICKY
    }

    private fun startSyncLoop() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                performSync()
                delay(SYNC_INTERVAL)
            }
        }
    }

    private suspend fun performSync() {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repo = CloudSyncRepository(applicationContext)
            
            val result = repo.performFullSync()
            
            if (result.downloaded > 0) {
                sendBroadcast(Intent(ACTION_DATA_UPDATED))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopService() {
        job?.cancel()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        scope.cancel()
    }

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_SYNC_NOW = "SYNC_NOW"
        const val ACTION_DATA_UPDATED = "com.example.saliguri.DATA_UPDATED"
        private const val SYNC_INTERVAL = 30_000L // 30 detik
        
        fun start(context: Context) {
            val intent = Intent(context, SyncService::class.java).apply {
                action = ACTION_START
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        fun stop(context: Context) {
            try {
                context.startService(Intent(context, SyncService::class.java).apply {
                    action = ACTION_STOP
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        fun startSyncNow(context: Context) {
            try {
                context.startService(Intent(context, SyncService::class.java).apply {
                    action = ACTION_SYNC_NOW
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
