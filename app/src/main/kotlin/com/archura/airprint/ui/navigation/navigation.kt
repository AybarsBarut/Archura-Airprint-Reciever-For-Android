package com.archura.airprint.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.archura.airprint.ui.screens.received_images.ReceivedImageDetailScreen
import com.archura.airprint.ui.screens.home.HomeScreen
import com.archura.airprint.ui.screens.settings.SettingsScreen

private const val HOME_ROUTE = "home"
private const val IMAGE_DETAIL_ROUTE = "received_image"
private const val IMAGE_ID_ARG = "imageId"
private const val SETTINGS_ROUTE = "settings"

@Composable
fun AirPrintNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
    ) {
        composable(HOME_ROUTE) {
            HomeScreen(
                onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                onOpenImage = { imageId ->
                    navController.navigate("$IMAGE_DETAIL_ROUTE/${Uri.encode(imageId)}")
                },
            )
        }
        composable("$IMAGE_DETAIL_ROUTE/{$IMAGE_ID_ARG}") {
            ReceivedImageDetailScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
