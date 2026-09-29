package com.stitten.stitteniptv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stitten.stitteniptv.ui.screens.*
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val SERIES_DETAILS = "series/{seriesId}"
    const val PLAYER = "player/{url}/{title}"

    fun seriesDetails(id: String) = "series/$id"
    fun player(url: String, title: String) =
        "player/${URLEncoder.encode(url, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.DASHBOARD) { DashboardScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(
            Routes.SERIES_DETAILS,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("seriesId") ?: ""
            SeriesDetailsScreen(navController, id)
        }
        composable(
            Routes.PLAYER,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
            val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
            PlayerScreen(navController, url, title)
        }
    }
}
