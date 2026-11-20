package com.lifeos.core.system.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lifeos.core.system.notification.LifeOsNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class ActiveTimerSnapshot(
    val isRunning: Boolean = false,
    val label: String = "",
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0
) {
    val formattedRemaining: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }

    val progressFraction: Float
        get() = if (totalSeconds > 0) {
            remainingSeconds.toFloat() / totalSeconds.toFloat()
        } else {
            0f
        }
}

/**
 * Serviço em Primeiro Plano (`foregroundServiceType="specialUse"`) para cronômetros
 * ininterruptos de descanso de calistenia (90s) e blocos Pomodoro (25m / 50m) na tela de bloqueio,
 * com vibração tátil dupla e bip acústico discreto ao zerar.
 */
class ActiveExecutionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var countdownJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopTimerAndSelf()
            ACTION_START -> {
                val seconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 90).coerceAtLeast(1)
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "Descanso de Série"
                startCountdownForeground(label = label, durationSeconds = seconds)
            }
        }
        return START_NOT_STICKY
    }

    private fun startCountdownForeground(label: String, durationSeconds: Int) {
        LifeOsNotificationChannels.ensureCreated(this)
        val initialNotification = buildNotification(label, durationSeconds)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, initialNotification)
        }

        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            for (remaining in durationSeconds downTo 1) {
                _timerState.value = ActiveTimerSnapshot(
                    isRunning = true,
                    label = label,
                    totalSeconds = durationSeconds,
                    remainingSeconds = remaining
                )
                manager.notify(FOREGROUND_NOTIFICATION_ID, buildNotification(label, remaining))
                delay(1_000L)
            }

            triggerCompletionCue(label, manager)

            _timerState.value = ActiveTimerSnapshot(isRunning = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun triggerCompletionCue(label: String, manager: NotificationManager) {
        // 1. Vibração tátil dupla (140ms pulso - 90ms pausa - 180ms pulso)
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0L, 140L, 90L, 180L), -1)
                )
            }
        }

        // 2. Bip sonoro discreto
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 240)
        }

        // 3. Alerta de retomada imediata
        val completionText = if (label.contains("Pomodoro", ignoreCase = true)) {
            "Bloco de foco profundo concluído! Levante-se e hidrate-se."
        } else {
            "Hora da próxima série! Mantenha a cadência do treino."
        }
        val doneNotification = NotificationCompat.Builder(this, LifeOsNotificationChannels.CHANNEL_EXACT_ALARMS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Life OS · $label Finalizado")
            .setContentText(completionText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(COMPLETION_NOTIFICATION_ID, doneNotification)
    }

    private fun stopTimerAndSelf() {
        countdownJob?.cancel()
        _timerState.value = ActiveTimerSnapshot(isRunning = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(label: String, remainingSeconds: Int): Notification {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)

        return NotificationCompat.Builder(this, LifeOsNotificationChannels.CHANNEL_ACTIVE_EXECUTION)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Life OS · $label")
            .setContentText("Contagem na tela de bloqueio: $formatted")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.lifeos.core.system.action.START_TIMER"
        private const val ACTION_STOP = "com.lifeos.core.system.action.STOP_TIMER"
        private const val EXTRA_DURATION_SECONDS = "EXTRA_DURATION_SECONDS"
        private const val EXTRA_LABEL = "EXTRA_LABEL"
        private const val FOREGROUND_NOTIFICATION_ID = 4202
        private const val COMPLETION_NOTIFICATION_ID = 4203

        private val _timerState = MutableStateFlow(ActiveTimerSnapshot())
        val timerState: StateFlow<ActiveTimerSnapshot> = _timerState.asStateFlow()

        fun startTimer(context: Context, label: String, durationSeconds: Int) {
            val intent = Intent(context, ActiveExecutionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, ActiveExecutionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
