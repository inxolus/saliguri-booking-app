package com.example.saliguri.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.services.SyncService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SyncService.start(context)
        }
    }
}
