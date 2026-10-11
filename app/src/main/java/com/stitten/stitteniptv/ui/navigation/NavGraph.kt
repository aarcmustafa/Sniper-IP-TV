package com.stitten.stitteniptv.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stitten.stitteniptv.ui.favorites.FavoritesScreen
import com.stitten.stitteniptv.ui.live.LiveScreen
import com.stitten.stitteniptv.ui.screens.*
import com.stitten.stitteniptv.ui.search.SearchScreen
import com.stitten.stitteniptv.ui.vod.MovieDetailsScreen
import com.stitten.stitteniptv.ui.vod.MoviesScreen
import com.stitten.stitteniptv.ui.vod.SeriesListScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val ADVANCED_SETTINGS = "advanced_settings"
    const val SERIES_DETAILS = "series/{seriesId}"
    const val PLAYER = "player/{url}/{title}"
    const val EPG = "epg/{channelId}/{channelName}/{epgUrl}"
    const val PARENTAL = "parental"
    const val ERRORS = "errors"
    const val SOURCES = "sources"
    const val ADD_SOURCE = "add_source"
    const val ADD_SOURCE_EDIT = "add_source/edit/{sourceId}"
    const val EXTERNAL_PLAYERS = "external_players"

    const val LIVE = "live"
    const val MOVIES = "movies"
    const val SERIES_LIST = "series_list"
    const val MOVIE_DETAILS = "movie/{movieId}"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"

    fun seriesDetails(id: String) = "series/$id"
    fun player(url: String, title: String) =
        "player/${URLEncoder.encode(url, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}"
    fun epg(channelId: String, channelName: String, epgUrl: String) =
        "epg/$channelId/${URLEncoder.encode(channelName, "UTF-8")}/${URLEncoder.encode(epgUrl, "UTF-8")}"
    fun addSourceEdit(id: Long) = "add_source/edit/$id"
    fun movieDetails(id: String) = "movie/$id"
    fun addSourceWithType(type: String) = "add_source?type=$type"
}

// ═══════════════════════════════════════════════════════════════
//  الانتقالات
// ═══════════════════════════════════════════════════════════════

private const val TRANSITION_MS = 350

private val enterSlide: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.EnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(TRANSITION_MS),
    ) + fadeIn(animationSpec = tween(TRANSITION_MS))
}

private val exitSlide: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.ExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(TRANSITION_MS),
    ) + fadeOut(animationSpec = tween(TRANSITION_MS))
}

private val popEnterSlide: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.EnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(TRANSITION_MS),
    ) + fadeIn(animationSpec = tween(TRANSITION_MS))
}

private val popExitSlide: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.ExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(TRANSITION_MS),
    ) + fadeOut(animationSpec = tween(TRANSITION_MS))
}

private val fadeOnlyEnter: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.EnterTransition = {
    fadeIn(animationSpec = tween(300)) +
            scaleIn(initialScale = 0.96f, animationSpec = tween(300))
}

private val fadeOnlyExit: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.ExitTransition = {
    fadeOut(animationSpec = tween(250)) +
            scaleOut(targetScale = 0.98f, animationSpec = tween(250))
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = enterSlide,
        exitTransition = exitSlide,
        popEnterTransition = popEnterSlide,
        popExitTransition = popExitSlide,
    ) {

        composable(Routes.SPLASH) { SplashScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.DASHBOARD) { DashboardScreen(navController) }

        composable(Routes.LIVE) { LiveScreen(navController) }
        composable(Routes.MOVIES) { MoviesScreen(navController) }
        composable(Routes.SERIES_LIST) { SeriesListScreen(navController) }
        composable(Routes.SEARCH) { SearchScreen(navController) }
        composable(Routes.FAVORITES) { FavoritesScreen(navController) }

        composable(
            Routes.MOVIE_DETAILS,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { MovieDetailsScreen(navController) }

        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.ADVANCED_SETTINGS) { AdvancedSettingsScreen(navController) }
        composable(Routes.PARENTAL) { ParentalControlScreen(navController) }
        composable(Routes.ERRORS) { ErrorLogScreen(navController) }

        composable(Routes.SOURCES) { SourcesManagerScreen(navController) }
        composable(Routes.EXTERNAL_PLAYERS) { ExternalPlayersScreen(navController) }

        // ✅ ADD_SOURCE مع دعم type
        composable(
            "${Routes.ADD_SOURCE}?type={type}",
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entry ->
            val type = entry.arguments?.getString("type") ?: ""
            val initialMode = when (type.uppercase()) {
                "XTREAM" -> 2
                "M3U", "M3U8" -> 0
                else -> 0
            }
            AddSourceScreen(
                navController = navController,
                editSourceId = -1L,
                initialMode = initialMode,
            )
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
            ),
            enterTransition = fadeOnlyEnter,
            exitTransition = fadeOnlyExit,
            popEnterTransition = fadeOnlyEnter,
            popExitTransition = fadeOnlyExit,
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
