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
    const val ADVANCED_SETTINGS = "advanced_settings"
    const val CONNECTION_LOG = "connection_log"
    const val SERIES_DETAILS = "series/{seriesId}"
    const val PLAYER = "player/{url}/{title}"
    const val EPG = "epg/{channelId}/{channelName}/{epgUrl}"
    const val PARENTAL = "parental"
    const val ERRORS = "errors"
    const val SOURCES = "sources"
    const val ADD_SOURCE = "add_source"
    const val ADD_SOURCE_EDIT = "add_source/edit/{sourceId}"
    const val EXTERNAL_PLAYERS = "external_players"

    fun seriesDetails(id: String) = "series/$id"
    fun player(url: String, title: String) =
        "player/${URLEncoder.encode(url, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}"
    fun epg(channelId: String, channelName: String, epgUrl: String) =
        "epg/$channelId/${URLEncoder.encode(channelName, "UTF-8")}/${URLEncoder.encode(epgUrl, "UTF-8")}"
    fun addSourceEdit(id: Long) = "add_source/edit/$id"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.DASHBOARD) { DashboardScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.ADVANCED_SETTINGS) { AdvancedSettingsScreen(navController) }
        composable(Routes.CONNECTION_LOG) { ConnectionLogScreen(navController) }
        composable(Routes.PARENTAL) { ParentalControlScreen(navController) }
        composable(Routes.ERRORS) { ErrorLogScreen() }
        composable(Routes.SOURCES) { SourcesManagerScreen(navController) }
        composable(Routes.EXTERNAL_PLAYERS) { ExternalPlayersScreen(navController) }
        composable(Routes.ADD_SOURCE) {
            AddSourceScreen(navController, editSourceId = -1L)
        }
        composable(
            Routes.ADD_SOURCE_EDIT,
            arguments = listOf(navArgument("sourceId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("sourceId") ?: -1L
            AddSourceScreen(navController, editSourceId = id)
        }
        composable(
            Routes.SERIES_DETAILS,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("seriesId") ?: ""
            SeriesDetailsScreen(navController, id)
        }
        composable(
            Routes.PLAYER,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { entry ->
            val url = URLDecoder.decode(entry.arguments?.getString("url") ?: "", "UTF-8")
            val title = URLDecoder.decode(entry.arguments?.getString("title") ?: "", "UTF-8")
            PlayerScreen(navController, url, title)
        }
        composable(
            Routes.EPG,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType },
                navArgument("channelName") { type = NavType.StringType },
                navArgument("epgUrl") { type = NavType.StringType }
            )
        ) { entry ->
            val cid = entry.arguments?.getString("channelId") ?: ""
            val cname = URLDecoder.decode(entry.arguments?.getString("channelName") ?: "", "UTF-8")
            val epgUrl = URLDecoder.decode(entry.arguments?.getString("epgUrl") ?: "", "UTF-8")
            EpgScreen(navController, cid, cname, epgUrl)
        }
    }
}
