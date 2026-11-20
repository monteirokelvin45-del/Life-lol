package com.lifeos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tokens visuais OLED 120 FPS do Life OS (Pilar 8.1 + Referências 01..10).
 */
object LifeOsTheme {
    val PureBlack = Color(0xFF000000)       // OLED Background
    val SurfaceDark = Color(0xFF0D0D0D)     // Bento Grid Card Base
    val SurfaceElevated = Color(0xFF141414) // Containers internos e células vazias
    val BorderSubtle = Color(0xFF1F1F1F)    // Bordas de 1dp

    // Cores Funcionais de Domínio
    val GreenHealth = Color(0xFF10E05B)     // Hábitos e Saúde
    val OrangeStreak = Color(0xFFFF4800)    // Streaks de dopamina e fogo
    val PurpleStudy = Color(0xFF8B5CF6)     // Estudos e Ebbinghaus
    val CyanFinance = Color(0xFF00E5FF)     // Entradas financeiras / Saldo
    val RedExpense = Color(0xFFFF2A4B)      // Despesas
    val AmberFocus = Color(0xFFFFB800)      // Metas diárias e destaques

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8E8E93)
    val TextMuted = Color(0xFF52525B)

    fun parseHexColorOrDefault(hex: String, fallback: Color = GreenHealth): Color {
        return runCatching {
            val cleaned = hex.trim().removePrefix("#")
            val argb = when (cleaned.length) {
                6 -> 0xFF000000L or cleaned.toLong(16)
                8 -> cleaned.toLong(16)
                else -> return fallback
            }
            Color(argb)
        }.getOrDefault(fallback)
    }
}

private val LifeOsDarkColors = darkColorScheme(
    primary = LifeOsTheme.GreenHealth,
    secondary = LifeOsTheme.PurpleStudy,
    tertiary = LifeOsTheme.CyanFinance,
    background = LifeOsTheme.PureBlack,
    surface = LifeOsTheme.SurfaceDark,
    surfaceVariant = LifeOsTheme.SurfaceElevated,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = LifeOsTheme.TextPrimary,
    onSurface = LifeOsTheme.TextPrimary,
    outline = LifeOsTheme.BorderSubtle,
    error = LifeOsTheme.RedExpense
)

private val LifeOsTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp
    )
)

@Composable
fun LifeOsMaterialTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LifeOsDarkColors,
        typography = LifeOsTypography,
        content = content
    )
}
