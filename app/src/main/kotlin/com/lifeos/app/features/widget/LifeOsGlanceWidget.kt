package com.lifeos.app.features.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lifeos.app.MainActivity
import com.lifeos.app.R
import com.lifeos.core.database.LifeOsDatabase
import com.lifeos.core.database.entity.HabitLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

private data class WidgetSnapshot(
    val topStreak: Int,
    val routineSummary: String,
    val weekCompletionFlags: List<Boolean>,
    val quickHabitLabel: String
)

/**
 * Widget 4x2 interativo em Jetpack Glance (Pilar 7.3 + Referência 07 + Jornada 24h):
 * - Exibe Streak ativo (`🔥 33 DIAS`), bolinhas da semana (`Seg..Dom`) e treino do dia.
 * - Botão `✓ Check` executa `QuickWidgetCheckInCallback` direto no banco sem abrir o app.
 */
class LifeOsGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) {
            runCatching {
                val dao = LifeOsDatabase.getInstance(context).lifeOsDao()
                val today = LocalDate.now()
                val monday = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
                val sunday = monday.plusDays(6)

                val habits = dao.getAllHabits().firstOrNull().orEmpty()
                val maxStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
                val routine = dao.getRoutineForDay(today.dayOfWeek.value).firstOrNull()
                val weekLogs = dao.getHabitLogsInWindow(monday.toEpochDay(), sunday.toEpochDay())
                    .firstOrNull()
                    .orEmpty()
                val activeEpochDays = weekLogs.filter { it.completed }.map { it.epochDay }.toSet()

                val flags = (0L..6L).map { offset ->
                    activeEpochDays.contains(monday.plusDays(offset).toEpochDay())
                }

                WidgetSnapshot(
                    topStreak = maxStreak,
                    routineSummary = routine?.name ?: "Mobilidade & Recuperação",
                    weekCompletionFlags = flags,
                    quickHabitLabel = habits.firstOrNull()?.name ?: "Hábito do Dia"
                )
            }.getOrDefault(
                WidgetSnapshot(
                    topStreak = 0,
                    routineSummary = "Abra o Life OS para iniciar",
                    weekCompletionFlags = List(7) { false },
                    quickHabitLabel = "Check-in"
                )
            )
        }

        val dayLabels = listOf("S", "T", "Q", "Q", "S", "S", "D")

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_background_squircle))
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(14.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 ${snapshot.topStreak} DIAS",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFF4800)),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "✓ ${snapshot.quickHabitLabel.take(14)}",
                        modifier = GlanceModifier
                            .clickable(actionRunCallback<QuickWidgetCheckInCallback>())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF10E05B)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dayLabels.forEachIndexed { idx, label ->
                        val done = snapshot.weekCompletionFlags.getOrElse(idx) { false }
                        Text(
                            text = if (done) "●$label " else "○$label ",
                            style = TextStyle(
                                color = ColorProvider(
                                    if (done) Color(0xFF10E05B) else Color(0xFF6E6E73)
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = "Hoje: ${snapshot.routineSummary}",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

class QuickWidgetCheckInCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val dao = LifeOsDatabase.getInstance(context).lifeOsDao()
                val today = LocalDate.now()
                val firstHabit = dao.getAllHabits().firstOrNull()?.firstOrNull() ?: return@withContext
                dao.insertHabitLog(
                    HabitLogEntity(
                        habitId = firstHabit.id,
                        epochDay = today.toEpochDay(),
                        completed = true,
                        valueLogged = firstHabit.dailyGoalValue
                    )
                )
                val nextStreak = firstHabit.currentStreak + 1
                dao.updateHabitStreak(
                    habitId = firstHabit.id,
                    currentStreak = nextStreak,
                    longestStreak = maxOf(firstHabit.longestStreak, nextStreak)
                )
            }
        }
        LifeOsGlanceWidget().update(context, glanceId)
    }
}

class LifeOsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LifeOsGlanceWidget()
}
