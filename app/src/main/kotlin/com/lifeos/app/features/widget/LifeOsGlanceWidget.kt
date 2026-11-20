package com.lifeos.app.features.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Widget de Tela Inicial 4x2 em Jetpack Glance (Pilar 7.3 + Referência 07):
 * Exibe o maior Streak ativo (`🔥 N DIAS`), a rotina fisiológica do dia e abre o Life OS com 1 toque.
 */
class LifeOsGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (topStreak, routineSummary, dueReviewsCount) = withContext(Dispatchers.IO) {
            runCatching {
                val dao = LifeOsDatabase.getInstance(context).lifeOsDao()
                val today = LocalDate.now()
                val habits = dao.getAllHabits().firstOrNull().orEmpty()
                val maxStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
                val routine = dao.getRoutineForDay(today.dayOfWeek.value).firstOrNull()
                val due = dao.getDueStudyReviews(today.toEpochDay()).firstOrNull().orEmpty().size
                Triple(
                    maxStreak,
                    routine?.name ?: "Recuperação Ativa & Mobilidade",
                    due
                )
            }.getOrDefault(Triple(0, "Abra o Life OS para iniciar", 0))
        }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_background_squircle))
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 $topStreak DIAS",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFF4800)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "$dueReviewsCount revisões",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF8B5CF6)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = "Hoje: $routineSummary",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = "Toque para registrar check-in rápido →",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF10E05B)),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

class LifeOsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LifeOsGlanceWidget()
}
