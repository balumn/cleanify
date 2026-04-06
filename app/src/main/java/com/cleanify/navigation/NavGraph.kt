package com.cleanify.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cleanify.feature.dashboard.DashboardScreen
import com.cleanify.feature.selection.AppSelectionScreen

private const val NavTweenMs = 320

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = CleanifyDestinations.DashboardRoute,
        modifier = modifier,
    ) {
        composable(
            route = CleanifyDestinations.DashboardRoute,
            enterTransition = {
                fadeIn(animationSpec = tween(NavTweenMs)) +
                    slideInHorizontally(animationSpec = tween(NavTweenMs + 40)) { it / 5 }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(220)) +
                    slideOutHorizontally(animationSpec = tween(260)) { -(it / 6) }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(260)) +
                    slideInHorizontally(animationSpec = tween(NavTweenMs)) { -(it / 5) }
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(220)) +
                    slideOutHorizontally(animationSpec = tween(280)) { it / 5 }
            },
        ) {
            DashboardScreen(
                onEditApps = { navController.navigate(CleanifyDestinations.SelectionRoute) },
            )
        }
        composable(
            route = CleanifyDestinations.SelectionRoute,
            enterTransition = {
                fadeIn(animationSpec = tween(NavTweenMs)) +
                    slideInHorizontally(animationSpec = tween(NavTweenMs + 60)) { it }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(220)) +
                    slideOutHorizontally(animationSpec = tween(280)) { -(it / 4) }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(260)) +
                    slideInHorizontally(animationSpec = tween(NavTweenMs)) { -(it / 4) }
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(220)) +
                    slideOutHorizontally(animationSpec = tween(NavTweenMs + 40)) { it }
            },
        ) {
            AppSelectionScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
