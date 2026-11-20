package com.lifeos.core.system.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lifeos.core.system.notification.LifeOsNotificationChannels

class ExactAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        LifeOsNotificationChannels.ensureCreated(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Life OS · Janela Crítica"
        val message = intent.getStringExtra(EXTRA_MESSAGE)
            ?: "Hora de consolidar sua revisão espaçada ou iniciar o treino."
        val notificationId = intent.getIntExtra(EXTRA_ID, DEFAULT_NOTIFICATION_ID)

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        val contentPendingIntent = launchIntent?.let {
            android.app.PendingIntent.getActivity(
                context,
                notificationId,
                it,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(
            context,
            LifeOsNotificationChannels.CHANNEL_EXACT_ALARMS
        )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_ID = "EXTRA_ID"
        private const val DEFAULT_NOTIFICATION_ID = 4201
    }
}
