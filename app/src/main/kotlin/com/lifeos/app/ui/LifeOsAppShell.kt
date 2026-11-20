package com.lifeos.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    val icon: ImageVector
) {
    HOME("home", "Hoje", Icons.Rounded.Dashboard),
    HABITS("habits", "Hábitos", Icons.Rounded.GridView),
    WORKOUT("workout", "Treino", Icons.Rounded.FitnessCenter),
    STUDY("study", "Estudos", Icons.Rounded.AutoStories),
    FINANCE("finance", "Carteira", Icons.Rounded.AccountBalanceWallet)
}

@Composable
fun LifeOsAppShell() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: LifeOsDestination.HOME.route

    Scaffold(
        containerColor = LifeOsTheme.PureBlack,
        bottomBar = {
            NavigationBar(
                containerColor = LifeOsTheme.SurfaceDark,
                tonalElevation = 0.dp
            ) {
                LifeOsDestination.entries.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.label
                            )
                        },
                        label = {
                            Text(dest.label, fontSize = 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LifeOsTheme.GreenHealth,
                            selectedTextColor = Color.White,
                            indicatorColor = LifeOsTheme.SurfaceElevated,
                            unselectedIconColor = LifeOsTheme.TextSecondary,
                            unselectedTextColor = LifeOsTheme.TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LifeOsDestination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(LifeOsDestination.HOME.route) {
                HomeScreen(viewModel = hiltViewModel())
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
