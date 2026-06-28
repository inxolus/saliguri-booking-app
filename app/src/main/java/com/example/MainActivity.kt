package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.FcmManager
import com.example.services.SyncService
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted.")
        } else {
            Log.d("MainActivity", "Notification permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        askNotificationPermission()

        // Register FCM & kirim ke cloud
        lifecycleScope.launch {
            registerFcmToken()
        }

        // Start sync service
        SyncService.start(this)

        val navigateToBookings = intent?.getBooleanExtra("highlight_new", false) == true

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val startDest = if (navigateToBookings) "bookings" else "login"
                    AppNavigation(startDestination = startDest)
                }
            }
        }
    }

    private suspend fun registerFcmToken() {
    try {
        Log.d("FCM_DEBUG", "Mulai ambil token...")
        
        val token = FirebaseMessaging.getInstance().token.await()
        Log.d("FCM_DEBUG", "Token berhasil: $token")
        
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.d("FCM_DEBUG", "Device ID: $deviceId")
        
        // Kirim ke cloud
        val fcmManager = FcmManager(this@MainActivity)
        fcmManager.registerDeviceToCloud(deviceId, token)
        Log.d("FCM_DEBUG", "Register ke cloud selesai")
        
    } catch (e: Exception) {
        Log.e("FCM_DEBUG", "ERROR: ${e.message}")
        e.printStackTrace()
    }
}

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
