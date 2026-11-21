package com.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.ui.theme.LifeOsTheme

/**
 * Superfície Bento OLED de Luxo com iluminação especular de topo (vidro fumê obsidiana)
 * e borda em gradiente luminoso sutil (Zero-GC).
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LifeOsTheme.SurfaceDark,
    borderColor: Color = LifeOsTheme.BorderSubtle,
    glowAccent: Color? = null,
    cornerRadius: Dp = 26.dp,
    contentPadding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isCustomTint = borderColor != LifeOsTheme.BorderSubtle
    val rimBrush = if (isCustomTint) {
        Brush.linearGradient(
            colors = listOf(
                borderColor.copy(alpha = 0.72f),
                borderColor.copy(alpha = 0.22f),
                LifeOsTheme.BorderSubtle.copy(alpha = 0.55f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                LifeOsTheme.BorderSpecular.copy(alpha = 0.75f),
                LifeOsTheme.BorderSubtle.copy(alpha = 0.85f),
                Color(0xFF14161D)
            )
        )
    }

    val surfaceBrush = Brush.verticalGradient(
        colors = listOf(
            if (backgroundColor == LifeOsTheme.SurfaceDark) LifeOsTheme.SurfaceGlassTop else backgroundColor,
            backgroundColor
        )
    )

    val ambientTint = glowAccent ?: if (isCustomTint) borderColor else null

    val baseMod = modifier
        .clip(shape)
        .background(brush = surfaceBrush)
        .let { m ->
            if (ambientTint != null) {
                m.drawWithCache {
                    val glowBrush = Brush.radialGradient(
                        colors = listOf(
                            ambientTint.copy(alpha = 0.13f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, 0f),
                        radius = size.minDimension * 0.95f
                    )
                    onDrawBehind {
                        drawRect(brush = glowBrush)
                    }
                }
            } else {
                m
            }
        }
        .border(width = 1.dp, brush = rimBrush, shape = shape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }

    Column(
        modifier = baseMod.padding(contentPadding),
        content = content
    )
}

/**
 * Orbe vetorial bioluminescente estilo Apple / Linear (substitui emojis crus por ícones de precisão).
 */
@Composable
fun GlowingIconOrb(
    icon: ImageVector,
    tint: Color,
    size: Dp = 40.dp,
    iconSize: Dp = 19.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.28f),
                        tint.copy(alpha = 0.08f)
                    )
                )
            )
            .border(1.dp, tint.copy(alpha = 0.36f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Pílula editorial de cabeçalho (Eyebrow Badge) com ponto luminoso e proteção contra quebra de linha.
 */
@Composable
fun EyebrowCapsule(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.11f))
            .border(1.dp, accent.copy(alpha = 0.28f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Text(
            text = text,
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.9.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Cápsula de busca frameless em vidro fumê (substitui OutlinedTextField pesado).
 */
@Composable
fun SleekSearchBarPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(LifeOsTheme.SurfaceGlassTop)
            .border(1.dp, LifeOsTheme.BorderSubtle, CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = LifeOsTheme.TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = LifeOsTheme.TextMuted,
                    fontSize = 13.5.sp,
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(LifeOsTheme.GreenHealth),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Pílula numérica anti-quebra de linha ("120 FPS", "Nv. 84", etc.).
 */
@Composable
fun LuxuryStatBadge(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.13f))
            .border(1.dp, accent.copy(alpha = 0.30f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false
        )
    }
}
