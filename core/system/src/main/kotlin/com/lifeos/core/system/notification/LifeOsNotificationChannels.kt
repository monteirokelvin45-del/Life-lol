package com.lifeos.core.system.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object LifeOsNotificationChannels {

    const val CHANNEL_EXACT_ALARMS = "lifeos_exact_alarms"
    const val CHANNEL_ACTIVE_EXECUTION = "lifeos_active_execution"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmChannel = NotificationChannel(
            CHANNEL_EXACT_ALARMS,
            "Revisões Ebbinghaus & Treinos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas exatos de estudo espaçado e janelas fisiológicas de treino."
            enableVibration(true)
        }

        val executionChannel = NotificationChannel(
            CHANNEL_ACTIVE_EXECUTION,
            "Cronômetro Ativo (Descanso & Pomodoro)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Contagem contínua em primeiro plano para séries de calistenia e foco."
            setShowBadge(false)
        }

        manager.createNotificationChannels(listOf(alarmChannel, executionChannel))
    }
}
