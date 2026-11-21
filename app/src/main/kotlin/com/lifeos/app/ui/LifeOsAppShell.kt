package com.lifeos.app.ui

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifeos.app.features.finance.FinanceScreen
import com.lifeos.app.features.habits.HabitsScreen
import com.lifeos.app.features.home.HomeScreen
import com.lifeos.app.features.study.StudyScreen
import com.lifeos.app.features.workout.WorkoutScreen
import com.lifeos.ui.theme.LifeOsTheme

private enum class LifeOsDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color
) {
    HOME("home", "Hoje", Icons.Rounded.Dashboard, LifeOsTheme.GreenHealth),
    HABITS("habits", "Hábitos", Icons.Rounded.GridView, LifeOsTheme.OrangeStreak),
    WORKOUT("workout", "Treino", Icons.Rounded.FitnessCenter, Color(0xFFB7F34D)),
    STUDY("study", "Estudos", Icons.Rounded.AutoStories, LifeOsTheme.PurpleStudy),
    FINANCE("finance", "Carteira", Icons.Rounded.AccountBalanceWallet, LifeOsTheme.CyanFinance)
}

@Composable
fun LifeOsAppShell(
    incomingSharedUri: Uri? = null,
    incomingSharedText: String? = null,
    requestedNavRoute: String? = null,
    onConsumedSharedPayload: () -> Unit = {},
    onConsumedNavRoute: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: LifeOsDestination.HOME.route
    val activeDest = LifeOsDestination.entries.find { it.route == currentRoute } ?: LifeOsDestination.HOME
    val auroraColor by animateColorAsState(
        targetValue = activeDest.accent,
        animationSpec = tween(durationMillis = 480),
        label = "tabAurora"
    )

    LaunchedEffect(incomingSharedUri, incomingSharedText) {
        if (incomingSharedUri != null || !incomingSharedText.isNullOrBlank()) {
            if (currentRoute != LifeOsDestination.HOME.route) {
                navController.navigate(LifeOsDestination.HOME.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    LaunchedEffect(requestedNavRoute) {
        if (!requestedNavRoute.isNullOrBlank()) {
            val valid = LifeOsDestination.entries.any { it.route == requestedNavRoute }
            if (valid) {
                navController.navigate(requestedNavRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            onConsumedNavRoute()
        }
    }

    Scaffold(
        containerColor = LifeOsTheme.PureBlack,
        bottomBar = {
            FloatingLuxuryGlassDock(
                currentRoute = currentRoute,
                onSelect = { dest ->
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LifeOsTheme.PureBlack)
                .drawWithCache {
                    val topAurora = Brush.radialGradient(
                        colors = listOf(
                            auroraColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.85f
                    )
                    onDrawBehind {
                        drawRect(brush = topAurora)
                    }
                }
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = LifeOsDestination.HOME.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(LifeOsDestination.HOME.route) {
                    HomeScreen(
                        viewModel = hiltViewModel(),
                        incomingSharedUri = incomingSharedUri,
                        incomingSharedText = incomingSharedText,
                        onConsumedSharedPayload = onConsumedSharedPayload
                    )
                }
                composable(LifeOsDestination.HABITS.route) {
                    HabitsScreen(viewModel = hiltViewModel())
                }
                composable(LifeOsDestination.WORKOUT.route) {
                    WorkoutScreen(viewModel = hiltViewModel())
                }
                composable(LifeOsDestination.STUDY.route) {
                    StudyScreen(viewModel = hiltViewModel())
                }
                composable(LifeOsDestination.FINANCE.route) {
                    FinanceScreen(viewModel = hiltViewModel())
                }
            }
        }
    }
}

@Composable
private fun FloatingLuxuryGlassDock(
    currentRoute: String,
    onSelect: (LifeOsDestination) -> Unit
) {
    val dockShape = RoundedCornerShape(30.dp)
    val rimBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.04f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        LifeOsTheme.PureBlack.copy(alpha = 0.92f),
                        LifeOsTheme.PureBlack
                    )
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(dockShape)
                .background(Color(0xFF101218).copy(alpha = 0.96f))
                .border(1.dp, rimBrush, dockShape)
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LifeOsDestination.entries.forEach { dest ->
                val selected = currentRoute == dest.route
                val itemShape = RoundedCornerShape(22.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(itemShape)
                        .background(
                            if (selected) dest.accent.copy(alpha = 0.15f) else Color.Transparent
                        )
                        .let { m ->
                            if (selected) {
                                m.border(1.dp, dest.accent.copy(alpha = 0.34f), itemShape)
                            } else {
                                m
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(dest) }
                        )
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label,
                        tint = if (selected) dest.accent else LifeOsTheme.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = dest.label,
                        color = if (selected) Color.White else LifeOsTheme.TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (selected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 14.dp, height = 2.5.dp)
                                .clip(CircleShape)
                                .background(dest.accent)
                        )
                    }
                }
            }
        }
    }
}
