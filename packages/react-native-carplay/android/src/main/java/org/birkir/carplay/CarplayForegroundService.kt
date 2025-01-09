package org.birkir.carplay

import android.util.Log
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.Context

class CarPlayForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun getMainActivityIntent(): Intent {
        Log.d(TAG, "getMainActivityIntent: packageName = $packageName")
        val packageManager = packageManager
        Log.d(TAG, "getMainActivityIntent: packageManager = $packageManager")
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        Log.d(TAG, "getMainActivityIntent: launchIntent = $launchIntent")
        return launchIntent ?: Intent() // Fallback to an empty intent if not found
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle the task here
        return START_STICKY
    }

    private fun createNotification(): Notification {
        Log.d(TAG, "createNotification: CHANNEL_ID = $CHANNEL_ID")
        val notificationIntent = getMainActivityIntent()
        Log.d(TAG, "createNotification: notificationIntent = $notificationIntent")
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        Log.d(TAG, "createNotification: pendingIntent = $pendingIntent")
        val mainAppContext: Context = applicationContext
        Log.d(TAG, "createNotification: mainAppContext = $mainAppContext")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CarPlay Service")
            .setContentText("Service is running in the foreground")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel: CHANNEL_ID = $CHANNEL_ID")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "createNotificationChannel: Build.VERSION.SDK_INT >= Build.VERSION_CODES.O")
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "CarPlay Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            Log.d(TAG, "createNotificationChannel: serviceChannel = $serviceChannel")
            val manager = getSystemService(NotificationManager::class.java)
            Log.d(TAG, "createNotificationChannel: manager = $manager")
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        var TAG = "CarPlayForegroundService"
        const val CHANNEL_ID = "CarPlayForegroundServiceChannel"
        const val NOTIFICATION_ID = 1
    }
}