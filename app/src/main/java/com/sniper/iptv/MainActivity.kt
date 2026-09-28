package com.sniper.iptv

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.sniper.iptv.data.*
import com.sniper.iptv.player.SmartAntiBufferingPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var playerManager: SmartAntiBufferingPlayerManager
    private lateinit var storage: LocalStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = LocalStorage(this)
        
        var playerStatusText by mutableStateOf("")
        playerManager = SmartAntiBufferingPlayerManager(this) { status ->
            playerStatusText = status
        }

        setContent {
            var settings by remember { mutableStateOf(storage.loadSettings()) }
            var profiles = remember { storage.loadProfiles() }
            var currentScreen by remember { mutableStateOf(if (profiles.isNotEmpty()) "dashboard" else "profiles") }

            var liveList by remember { mutableStateOf(listOf<MediaItemModel>()) }
            var movieList by remember { mutableStateOf(listOf<MediaItemModel>()) }
            var seriesList by remember { mutableStateOf(listOf<MediaItemModel>()) }
            var activeStream by remember { mutableStateOf<MediaItemModel?>(null) }
            var selectedSeries by remember { mutableStateOf<MediaItemModel?>(null) }
            var seasonsList by remember { mutableStateOf(listOf<SeriesSeasonModel>()) }
            var currentEpgList by remember { mutableStateOf(listOf<EpgProgram>()) }
            var favorites = remember { storage.loadFavorites() }

            Surface(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    "profiles" -> ProfilesManagementScreen(profiles) { newProf ->
                        if (profiles.size < 10) {
                            profiles.add(newProf)
                            storage.saveProfiles(profiles)
                        }
                    } onSelectProfile = { prof ->
                        settings = settings.copy(activeProfileId = prof.id)
                        storage.saveSettings(settings)
                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                            val repo = XtreamRepository(prof.serverUrl)
                            val (l, m, s) = repo.fetchAll(prof.username, prof.password)
                            liveList = l
                            movieList = m
                            seriesList = s
                            currentScreen = "dashboard"
                        }
                    }
                    "dashboard" -> DashboardScreen(
                        settings = settings,
                        onSelectCategory = { cat -> currentScreen = cat },
                        onManageProfiles = { currentScreen = "profiles" },
                        onOpenSettings = { currentScreen = "settings" }
                    )
                    "live" -> ContentScreen("Live TV", liveList, favorites, { currentScreen = "dashboard" }, { id -> favorites.add(id); storage.saveFavorites(favorites) }) { item ->
                        activeStream = item
                        playerManager.applyAntiBuffering(settings.lowQualityMode)
                        playerManager.play(item.streamUrl)
                        val activeProf = profiles.find { it.id == settings.activeProfileId }
                        if (activeProf != null) {
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                                val repo = XtreamRepository(activeProf.serverUrl)
                                currentEpgList = repo.fetchChannelEpg(activeProf.username, activeProf.password, item.extraId)
                            }
                        }
                        currentScreen = "player"
                    }
                    "movies" -> ContentScreen("Movies VOD", movieList, favorites, { currentScreen = "dashboard" }, { id -> favorites.add(id); storage.saveFavorites(favorites) }) { item ->
                        activeStream = item
                        playerManager.applyAntiBuffering(settings.lowQualityMode)
                        playerManager.play(item.streamUrl)
                        currentScreen = "player"
                    }
                    "series" -> ContentScreen("Series", seriesList, favorites, { currentScreen = "dashboard" }, { id -> favorites.add(id); storage.saveFavorites(favorites) }) { item ->
                        selectedSeries = item
                        val activeProf = profiles.find { it.id == settings.activeProfileId }
                        if (activeProf != null) {
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                                val repo = XtreamRepository(activeProf.serverUrl)
                                seasonsList = repo.fetchSeriesDetails(activeProf.username, activeProf.password, item.extraId)
                                currentScreen = "series_details"
                            }
                        }
                    }
                    "series_details" -> SeriesDetailsScreen(series = selectedSeries, seasons = seasonsList, onBack = { currentScreen = "series" }) { ep ->
                        activeStream = MediaItemModel(ep.id, ep.title, ep.streamUrl, selectedSeries?.posterUrl, "Series", MediaType.SERIES)
                        playerManager.applyAntiBuffering(settings.lowQualityMode)
                        playerManager.play(ep.streamUrl)
                        currentScreen = "player"
                    }
                    "player" -> VideoPlayerScreen(
                        stream = activeStream,
                        playerManager = playerManager,
                        statusText = playerStatusText,
                        liveChannels = liveList,
                        epgPrograms = currentEpgList,
                        onChannelSwitch = { newStream ->
                            activeStream = newStream
                            playerManager.play(newStream.streamUrl)
                            val activeProf = profiles.find { it.id == settings.activeProfileId }
                            if (activeProf != null) {
                                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                                    val repo = XtreamRepository(activeProf.serverUrl)
                                    currentEpgList = repo.fetchChannelEpg(activeProf.username, activeProf.password, newStream.extraId)
                                }
                            }
                        },
                        onBack = {
                            playerManager.player.stop()
                            currentScreen = "dashboard"
                        }
                    )
                    "settings" -> SettingsScreen(settings = settings, onUpdate = { newSet ->
                        settings = newSet
                        storage.saveSettings(newSet)
                        playerManager.applyAntiBuffering(newSet.lowQualityMode)
                    }, onBack = { currentScreen = "dashboard" })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}
@Composable
fun ProfilesManagementScreen(profiles: List<XtreamProfile>, onAddProfile: (XtreamProfile) -> Unit, onSelectProfile: (XtreamProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("http://") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "إدارة حتى 10 حسابات Xtream", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.height(150.dp)) {
            items(profiles) { prof ->
                Button(onClick = { onSelectProfile(prof) }, modifier = Modifier.fillMaxWidth().focusable()) {
                    Text("حساب: ${prof.name}")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (profiles.size < 10) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الحساب") }, modifier = Modifier.focusable())
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Server URL") }, modifier = Modifier.focusable())
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, modifier = Modifier.focusable())
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, modifier = Modifier.focusable())
            Button(onClick = { if (name.isNotBlank()) onAddProfile(XtreamProfile(name = name, serverUrl = url, username = user, password = pass)) }, modifier = Modifier.focusable()) {
                Text("إضافة الحساب")
            }
        }
    }
}

@Composable
fun DashboardScreen(settings: AppSettings, onSelectCategory: (String) -> Unit, onManageProfiles: () -> Unit, onOpenSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        val timeNow = SimpleDateFormat(if (settings.is24HourFormat) "HH:mm" else "hh:mm a", Locale.getDefault()).format(Date())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Sniper IPTV Pro + EPG", style = MaterialTheme.typography.headlineMedium)
            Text(text = timeNow, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { onSelectCategory("live") }, modifier = Modifier.size(140.dp, 80.dp).focusable()) { Text("البث المباشر") }
            Button(onClick = { onSelectCategory("movies") }, modifier = Modifier.size(140.dp, 80.dp).focusable()) { Text("الأفلام") }
            Button(onClick = { onSelectCategory("series") }, modifier = Modifier.size(140.dp, 80.dp).focusable()) { Text("المسلسلات") }
            Button(onClick = onManageProfiles, modifier = Modifier.size(140.dp, 80.dp).focusable()) { Text("الحسابات") }
            Button(onClick = onOpenSettings, modifier = Modifier.size(140.dp, 80.dp).focusable()) { Text("الإعدادات") }
        }
    }
}
@Composable
fun ContentScreen(title: String, items: List<MediaItemModel>, favorites: Set<String>, onBack: () -> Unit, onFavoriteToggle: (String) -> Unit, onSelect: (MediaItemModel) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "$title (${items.size})", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onBack, modifier = Modifier.focusable()) { Text("Back") }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                Surface(onClick = { onSelect(item) }, modifier = Modifier.fillMaxWidth().height(55.dp).focusable()) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = item.title)
                        Button(onClick = { onFavoriteToggle(item.id) }, modifier = Modifier.focusable()) {
                            Text(if (favorites.contains(item.id)) "★" else "☆")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesDetailsScreen(series: MediaItemModel?, seasons: List<SeriesSeasonModel>, onBack: () -> Unit, onSelectEpisode: (EpisodeModel) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = series?.title ?: "Series Details", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onBack, modifier = Modifier.focusable()) { Text("Back") }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(seasons) { season ->
                Text(text = "الموسم ${season.seasonNum}", style = MaterialTheme.typography.titleMedium)
                season.episodes.forEach { ep ->
                    Surface(onClick = { onSelectEpisode(ep) }, modifier = Modifier.fillMaxWidth().height(45.dp).focusable()) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                            Text(text = "الحلقة ${ep.episodeNum}: ${ep.title}")
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun VideoPlayerScreen(stream: MediaItemModel?, playerManager: SmartAntiBufferingPlayerManager, statusText: String, liveChannels: List<MediaItemModel>, epgPrograms: List<EpgProgram>, onChannelSwitch: (MediaItemModel) -> Unit, onBack: () -> Unit) {
    var showZappingList by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = playerManager.player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (epgPrograms.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(30.dp)) {
                Surface(modifier = Modifier.wrapContentSize()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "البرنامج الحالي: ${epgPrograms.first().title}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        if (statusText.isNotBlank()) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(30.dp)) {
                Surface(modifier = Modifier.wrapContentSize()) {
                    Text(text = statusText, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)) {
            Button(onClick = onBack, modifier = Modifier.focusable()) { Text("خروج") }
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = { showZappingList = !showZappingList }, modifier = Modifier.focusable()) { Text("القنوات (Zapping)") }
        }

        if (showZappingList) {
            Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(0.35f).padding(16.dp)) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "القنوات الحية ودليل البرامج", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(liveChannels) { ch ->
                                Surface(onClick = { onChannelSwitch(ch); showZappingList = false }, modifier = Modifier.fillMaxWidth().height(45.dp).focusable()) {
                                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.CenterStart) {
                                        Text(text = ch.title)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(settings: AppSettings, onUpdate: (AppSettings) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "الإعدادات وضبط منع التقطع والبروتوكولات", style = MaterialTheme.typography.headlineLarge)
        Button(onClick = { onUpdate(settings.copy(is24HourFormat = !settings.is24HourFormat)) }, modifier = Modifier.focusable()) {
            Text("صيغة الوقت: ${if (settings.is24HourFormat) "24 ساعة" else "12 ساعة"}")
        }
        Button(onClick = { onUpdate(settings.copy(lowQualityMode = !settings.lowQualityMode)) }, modifier = Modifier.focusable()) {
            Text("وضع منع التقطع (جودة 360p): ${if (settings.lowQualityMode) "مفعل" else "معطل"}")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onBack, modifier = Modifier.focusable()) { Text("رجوع") }
    }
}

