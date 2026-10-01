package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that restarts CricLiveBackgroundService when the phone boots up,
 * after app updates, or when an alarm triggers following task removal.
 */
class CricBootAndAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        Log.d(TAG, "Broadcast received: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            ACTION_RESTART_SYNC -> {
                Log.d(TAG, "Starting CricLiveBackgroundService from receiver: $action")
                CricLiveBackgroundService.start(context)
            }
        }
    }

    companion object {
        private const val TAG = "CricBootReceiver"
        const val ACTION_RESTART_SYNC = "com.example.ACTION_RESTART_SYNC_SERVICE"
    }
}
