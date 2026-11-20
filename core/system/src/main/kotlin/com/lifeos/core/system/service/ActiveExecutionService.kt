package com.lifeos.core.system.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
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
 * ininterruptos de descanso de calistenia e blocos Pomodoro de foco (Pilar 3).
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
            _timerState.value = ActiveTimerSnapshot(isRunning = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
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
            .setContentText("Tempo restante: $formatted")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
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
