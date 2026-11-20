package com.lifeos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.ui.theme.LifeOsTheme

/**
 * Container Bento Grid OLED com cantos squircle contínuos, superfície `#0D0D0D`
 * e borda sutil de 1dp `#1F1F1F` (Referências 01, 05, 08, 09 e 10).
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LifeOsTheme.SurfaceDark,
    borderColor: Color = LifeOsTheme.BorderSubtle,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickableModifier = if (onClick != null) {
        modifier.clip(shape).clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = clickableModifier,
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
