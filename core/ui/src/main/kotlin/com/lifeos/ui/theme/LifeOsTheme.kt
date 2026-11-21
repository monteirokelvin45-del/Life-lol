package com.lifeos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Design System OLED de Luxo 120 FPS do Life OS (Conselho de 45 Especialistas).
 * - PureBlack (#000000) desliga fisicamente os subpixels em telas AMOLED/OLED.
 * - Superfícies em Obsidiana Fria com iluminação especular de topo (Linear / Apple Watch Ultra).
 * - Tipografia editorial com numerais tabulares ("tnum") e tracking negativo em displays.
 */
object LifeOsTheme {
    val PureBlack = Color(0xFF000000)          // OLED Absoluto
    val SurfaceDark = Color(0xFF0B0C10)        // Base Bento Obsidiana Profunda
    val SurfaceGlassTop = Color(0xFF14161D)    // Luz especular superior sutil
    val SurfaceElevated = Color(0xFF161821)    // Pílulas internas e controles
    val BorderSubtle = Color(0xFF222530)       // Hairline 1dp
    val BorderSpecular = Color(0xFF343949)     // Reflexo superior de vidro

    // Paleta Bioluminescente Calibrada para Contraste OLED
    val GreenHealth = Color(0xFF12E86B)        // Saúde, Fisiologia & Confirmação
    val OrangeStreak = Color(0xFFFF5514)       // Dopamina, Fogo & Streaks
    val PurpleStudy = Color(0xFF9B6BFF)        // Cognição, IA & Ebbinghaus
    val CyanFinance = Color(0xFF00E5FF)        // Liquidez, Escudos & Cronobiologia
    val RedExpense = Color(0xFFFF3358)         // Vazamentos, Corte Cafeína & Fadiga
    val AmberFocus = Color(0xFFFFBE1A)         // Destaques Dourados & Regressão

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF9499A6)
    val TextMuted = Color(0xFF5A6070)

    private val currencyGroupingSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    fun formatMoney(amount: Double, currencyCode: String, decimals: Int = 0): String {
        val pattern = if (decimals > 0) "#,##0.00" else "#,##0"
        val formatted = DecimalFormat(pattern, currencyGroupingSymbols).format(amount)
        return "$formatted $currencyCode"
    }

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
        fontSize = 29.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.9).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontSize = 23.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.6).sp,
        fontFeatureSettings = "tnum"
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = "tnum"
    ),
    bodyMedium = TextStyle(
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal,
        fontFeatureSettings = "tnum"
    ),
    labelSmall = TextStyle(
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        fontFeatureSettings = "tnum"
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
