package com.stitten.stitteniptv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

enum class ContentType { LIVE, VOD, SERIES }

data class MediaItemData(
    val id: String,
    val name: String,
    val url: String,
    val type: ContentType,
    val group: String,
    val logo: String,
    val plot: String = "",
    val rating: String = "",
    var isFavorite: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SttitenIptvApp()
        }
    }
}

@Composable
fun SttitenIptvApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("SttitenPrefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    
    var currentScreen by remember { mutableStateOf("splash") }
    
    val mediaList = remember { mutableStateListOf<MediaItemData>() }
    var selectedMedia by remember { mutableStateOf<MediaItemData?>(null) }
    var seriesEpisodes by remember { mutableStateOf<List<MediaItemData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var serverUrl by remember { mutableStateOf(prefs.getString("serverUrl", "") ?: "") }
    var username by remember { mutableStateOf(prefs.getString("username", "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString("password", "") ?: "") }
    var m3uUrlInput by remember { mutableStateOf(prefs.getString("m3uUrl", "") ?: "") }
    
    var timeFormat24h by remember { mutableStateOf(prefs.getBoolean("timeFormat24h", true)) }
    var isExternalPlayer by remember { mutableStateOf(prefs.getBoolean("isExternalPlayer", false)) }
    var videoDecoder by remember { mutableStateOf(prefs.getString("videoDecoder", "Hardware") ?: "Hardware") }
    var parentalPin by remember { mutableStateOf(prefs.getString("parentalPin", "1234") ?: "1234") }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        when (currentScreen) {
            "splash" -> SplashScreen(
                onTimeout = {
                    currentScreen = if (serverUrl.isNotEmpty() || m3uUrlInput.isNotEmpty()) "dashboard" else "login"
                }
            )

            "login" -> LoginScreen(
                m3uUrl = m3uUrlInput, onM3uChange = { m3uUrlInput = it },
                serverUrl = serverUrl, onServerChange = { serverUrl = it },
                username = username, onUserChange = { username = it },
                password = password, onPassChange = { password = it },
                onLoadM3u = {
                    prefs.edit().putString("m3uUrl", m3uUrlInput).apply()
                    scope.launch {
                        isLoading = true
                        errorMessage = ""
                        val content = fetchUrlContent(m3uUrlInput)
                        if (content.isNotEmpty()) {
                            mediaList.clear()
                            mediaList.addAll(parseM3UContent(content))
                            currentScreen = "dashboard"
                        } else {
                            errorMessage = "فشل تحميل رابط M3U!"
                        }
                        isLoading = false
                    }
                },
                onLoadXtream = {
                    prefs.edit()
                        .putString("serverUrl", serverUrl)
                        .putString("username", username)
                        .putString("password", password)
                        .apply()
                    scope.launch {
                        isLoading = true
                        errorMessage = ""
                        val fetchedData = fetchAllXtreamData(serverUrl, username, password)
                        if (fetchedData.isNotEmpty()) {
                            mediaList.clear()
                            mediaList.addAll(fetchedData)
                            currentScreen = "dashboard"
                        } else {
                            errorMessage = "فشل الاتصال بسيرفر Xtream!"
                        }
                        isLoading = false
                    }
                },
                errorMessage = errorMessage
            )

            "dashboard" -> DashboardScreen(
                mediaItems = mediaList,
                timeFormat24h = timeFormat24h,
                onMediaSelected = { media ->
                    selectedMedia = media
                    if (media.type == ContentType.SERIES) {
                        scope.launch {
                            isLoading = true
                            seriesEpisodes = fetchSeriesEpisodes(serverUrl, username, password, media.id)
                            isLoading = false
                            currentScreen = "series_details"
                        }
                    } else if (isExternalPlayer) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(media.url), "video/*")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            errorMessage = "لا يوجد مشغل خارجي مثبت!"
                        }
                    } else {
                        currentScreen = "player"
                    }
                },
                onOpenSettings = { currentScreen = "settings" },
                onLogout = { currentScreen = "login" }
            )

            "series_details" -> SeriesDetailsScreen(
                seriesName = selectedMedia?.name ?: "المسلسل",
                episodes = seriesEpisodes,
                onEpisodeSelected = { episode ->
                    selectedMedia = episode
                    currentScreen = "player"
                },
                onBack = { currentScreen = "dashboard" }
            )

            "player" -> selectedMedia?.let { media ->
                PlayerScreen(
                    videoUrl = media.url,
                    videoDecoder = videoDecoder,
                    onBack = { currentScreen = "dashboard" }
                )
            }

            "settings" -> SettingsScreen(
                timeFormat24h = timeFormat24h,
                onTimeFormatChange = { 
                    timeFormat24h = it
                    prefs.edit().putBoolean("timeFormat24h", it).apply()
                },
                isExternalPlayer = isExternalPlayer,
                onPlayerTypeChange = { 
                    isExternalPlayer = it
                    prefs.edit().putBoolean("isExternalPlayer", it).apply()
                },
                videoDecoder = videoDecoder,
                onDecoderChange = { 
                    videoDecoder = it
                    prefs.edit().putString("videoDecoder", it).apply()
                },
                parentalPin = parentalPin,
                onPinChange = { 
                    parentalPin = it
                    prefs.edit().putString("parentalPin", it).apply()
                },
                onBack = { currentScreen = "dashboard" }
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "STTITEN IP TV",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = Color(0xFF38BDF8))
        }
    }
}
@Composable
fun LoginScreen(
    m3uUrl: String, onM3uChange: (String) -> Unit,
    serverUrl: String, onServerChange: (String) -> Unit,
    username: String, onUserChange: (String) -> Unit,
    password: String, onPassChange: (String) -> Unit,
    onLoadM3u: () -> Unit, onLoadXtream: () -> Unit, errorMessage: String
) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("STTITEN IP TV", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(value = m3uUrl, onValueChange = onM3uChange, label = { Text("رابط ملف M3U / M3U8") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onLoadM3u, modifier = Modifier.fillMaxWidth()) { Text("تحميل عبر رابط M3U") }
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = serverUrl, onValueChange = onServerChange, label = { Text("رابط سيرفر Xtream") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = username, onValueChange = onUserChange, label = { Text("اسم المستخدم") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = onPassChange, label = { Text("كلمة المرور") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onLoadXtream, modifier = Modifier.fillMaxWidth()) { Text("تسجيل الدخول عبر Xtream") }
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(errorMessage, color = Color.Red)
        }
    }
}

@Composable
fun DashboardScreen(
    mediaItems: List<MediaItemData>,
    timeFormat24h: Boolean,
    onMediaSelected: (MediaItemData) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ContentType.LIVE) }
    var selectedGroup by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    val currentTime = remember(timeFormat24h) {
        SimpleDateFormat(if (timeFormat24h) "HH:mm" else "hh:mm a", Locale.getDefault()).format(Date())
    }

    val groups = listOf("الكل", "المفضلة") + mediaItems.filter { it.type == selectedTab }.map { it.group }.distinct()
    val filteredItems = mediaItems.filter { item ->
        item.type == selectedTab &&
        (selectedGroup == "الكل" || (selectedGroup == "المفضلة" && item.isFavorite) || item.group == selectedGroup) &&
        item.name.contains(searchQuery, ignoreCase = true)
    }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        Column(modifier = Modifier.width(300.dp).fillMaxHeight().background(Color(0xFF1E293B)).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("STTITEN IP TV", color = Color(0xFF38BDF8), style = MaterialTheme.typography.titleMedium)
                Text(currentTime, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { selectedTab = ContentType.LIVE; selectedGroup = "الكل" }) { Text("بث حي") }
                Button(onClick = { selectedTab = ContentType.VOD; selectedGroup = "الكل" }) { Text("أفلام") }
                Button(onClick = { selectedTab = ContentType.SERIES; selectedGroup = "الكل" }) { Text("مسلسلات") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("الإعدادات المتقدمة") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("تسجيل الخروج") }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("بحث...") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(groups) { group ->
                    Surface(
                        color = if (group == selectedGroup) Color(0xFF2563EB) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { selectedGroup = group },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(text = group, color = Color.White, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
            if (selectedTab == ContentType.LIVE) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredItems) { item -> ChannelRowItem(item, onMediaSelected) }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredItems) { item -> VodGridItem(item, onMediaSelected) }
                }
            }
        }
    }
}
@Composable
fun ChannelRowItem(item: MediaItemData, onMediaSelected: (MediaItemData) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Surface(
        color = Color(0xFF1E293B),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .border(width = if (isFocused) 2.dp else 0.dp, color = if (isFocused) Color(0xFF38BDF8) else Color.Transparent, shape = MaterialTheme.shapes.small)
            .clickable { onMediaSelected(item) },
        shape = MaterialTheme.shapes.small
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.logo.isNotEmpty()) {
                    AsyncImage(model = item.logo, contentDescription = null, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column {
                    Text(text = item.name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(text = "يبث حالياً: برنامج مباشر", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = { item.isFavorite = !item.isFavorite }) {
                Text(if (item.isFavorite) "★" else "☆")
            }
        }
    }
}

@Composable
fun VodGridItem(item: MediaItemData, onMediaSelected: (MediaItemData) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF1E293B),
        modifier = Modifier
            .height(200.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .border(width = if (isFocused) 2.dp else 0.dp, color = if (isFocused) Color(0xFF38BDF8) else Color.Transparent, shape = MaterialTheme.shapes.medium)
            .clickable { onMediaSelected(item) },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            if (item.logo.isNotEmpty()) {
                AsyncImage(model = item.logo, contentDescription = null, modifier = Modifier.height(120.dp).fillMaxWidth())
            }
            Text(text = item.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

@Composable
fun SeriesDetailsScreen(seriesName: String, episodes: List<MediaItemData>, onEpisodeSelected: (MediaItemData) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(seriesName, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF38BDF8))
            Button(onClick = onBack) { Text("العودة") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(episodes) { ep ->
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEpisodeSelected(ep) },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = ep.name, color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
private var downloadCache: SimpleCache? = null

fun getSttitenCache(context: Context): SimpleCache {
    if (downloadCache == null) {
        val cacheDir = File(context.cacheDir, "sttiten_media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024) 
        val databaseProvider = StandaloneDatabaseProvider(context)
        downloadCache = SimpleCache(cacheDir, evictor, databaseProvider)
    }
    return downloadCache!!
}

@Composable
fun PlayerScreen(
    videoUrl: String, 
    videoDecoder: String = "Hardware", 
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableStateOf(0) }
    val maxRetries = 3

    val exoPlayer = remember {
        val cache = getSttitenCache(context)
        val upstreamFactory = DefaultHttpDataSource.Factory()
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))

        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(videoUrl) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isLoading = playbackState == Player.STATE_BUFFERING || 
                            playbackState == Player.STATE_IDLE
                
                if (playbackState == Player.STATE_READY) {
                    retryCount = 0
                    errorMessage = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (retryCount < maxRetries) {
                    retryCount++
                    errorMessage = "انقطع الاتصال. جاري إعادة المحاولة (محاولة $retryCount من $maxRetries)..."
                    
                    scope.launch {
                        delay(3000)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                } else {
                    errorMessage = "عذراً، تعذر الاتصال بالبث بعد عدة محاولات."
                    isLoading = false
                }
            }
        }
        
        exoPlayer.addListener(listener)
        
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = onBack,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f))
        ) {
            Text("العودة", color = Color.White)
        }

        if (isLoading && errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        }

        errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (retryCount <= maxRetries && error.contains("جاري إعادة المحاولة")) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = error, 
                        color = if (retryCount > maxRetries) Color.Red else Color.White, 
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (retryCount > maxRetries) {
                        Button(onClick = {
                            retryCount = 0
                            errorMessage = null
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        }) {
                            Text("إعادة المحاولة يدوياً")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                        Text("العودة للقائمة الرئيسية")
                    }
                }
            }
        }
    }
}
@Composable
fun SettingsScreen(
    timeFormat24h: Boolean, onTimeFormatChange: (Boolean) -> Unit,
    isExternalPlayer: Boolean, onPlayerTypeChange: (Boolean) -> Unit,
    videoDecoder: String, onDecoderChange: (String) -> Unit,
    parentalPin: String, onPinChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).padding(32.dp)) {
        Text("إعدادات STTITEN IP TV", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("صيغة الوقت (24 ساعة)", color = Color.White)
            Switch(checked = timeFormat24h, onCheckedChange = onTimeFormatChange)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("تشغيل عبر مشغل خارجي", color = Color.White)
            Switch(checked = isExternalPlayer, onCheckedChange = onPlayerTypeChange)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("فك التشفير: $videoDecoder", color = Color.White)
            Button(onClick = { onDecoderChange(if (videoDecoder == "Hardware") "Software" else "Hardware") }) { Text("تبديل") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = parentalPin, onValueChange = onPinChange, label = { Text("رمز الرقابة الأبوية (PIN)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("حفظ والعودة") }
    }
}
fun parseM3UContent(content: String): List<MediaItemData> {
    val items = mutableListOf<MediaItemData>()
    val lines = content.lines()
    var currentTitle = ""
    var currentGroup = "عام"
    var currentLogo = ""
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("#EXTINF:")) {
            Regex("group-title=\"([^\"]*)\"").find(trimmed)?.let { currentGroup = it.groupValues[1] }
            Regex("tvg-logo=\"([^\"]*)\"").find(trimmed)?.let { currentLogo = it.groupValues[1] }
            val commaIndex = trimmed.lastIndexOf(',')
            if (commaIndex != -1) currentTitle = trimmed.substring(commaIndex + 1).trim()
        } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
            items.add(MediaItemData(UUID.randomUUID().toString(), currentTitle.ifEmpty { "قناة" }, trimmed, ContentType.LIVE, currentGroup, currentLogo))
            currentTitle = ""; currentLogo = ""
        }
    }
    return items
}

suspend fun fetchUrlContent(urlString: String): String = withContext(Dispatchers.IO) {
    try { URL(urlString).readText() } catch (e: Exception) { "" }
}

suspend fun fetchAllXtreamData(server: String, user: String, pass: String): List<MediaItemData> = withContext(Dispatchers.IO) {
    val allItems = mutableListOf<MediaItemData>()
    try {
        val liveJson = JSONArray(URL("$server/player_api.php?username=$user&password=$pass&action=get_live_streams").readText())
        for (i in 0 until liveJson.length()) {
            val obj = liveJson.getJSONObject(i)
            val id = obj.optString("stream_id", "")
            allItems.add(MediaItemData(id, obj.optString("name", ""), "$server/live/$user/$pass/$id.ts", ContentType.LIVE, obj.optString("category_name", "بث حي"), obj.optString("stream_icon", "")))
        }
        val vodJson = JSONArray(URL("$server/player_api.php?username=$user&password=$pass&action=get_vod_streams").readText())
        for (i in 0 until vodJson.length()) {
            val obj = vodJson.getJSONObject(i)
            val id = obj.optString("stream_id", "")
            val ext = obj.optString("container_extension", "mp4")
            allItems.add(MediaItemData(id, obj.optString("name", ""), "$server/movie/$user/$pass/$id.$ext", ContentType.VOD, obj.optString("category_name", "أفلام"), obj.optString("stream_icon", ""), obj.optString("plot", ""), obj.optString("rating", "")))
        }
        val seriesJson = JSONArray(URL("$server/player_api.php?username=$user&password=$pass&action=get_series").readText())
        for (i in 0 until seriesJson.length()) {
            val obj = seriesJson.getJSONObject(i)
            val id = obj.optString("series_id", "")
            allItems.add(MediaItemData(id, obj.optString("name", ""), "", ContentType.SERIES, obj.optString("category_name", "مسلسلات"), obj.optString("cover", ""), obj.optString("plot", ""), obj.optString("rating", "")))
        }
    } catch (e: Exception) { e.printStackTrace() }
    allItems
}

suspend fun fetchSeriesEpisodes(server: String, user: String, pass: String, seriesId: String): List<MediaItemData> = withContext(Dispatchers.IO) {
    val episodesList = mutableListOf<MediaItemData>()
    try {
        val url = "$server/player_api.php?username=$user&password=$pass&action=get_series_info&series_id=$seriesId"
        val json = JSONObject(URL(url).readText())
        val episodesObj = json.optJSONObject("episodes")
        episodesObj?.keys()?.forEach { seasonKey ->
            val seasonArray = episodesObj.optJSONArray(seasonKey)
            if (seasonArray != null) {
                for (i in 0 until seasonArray.length()) {
                    val ep = seasonArray.getJSONObject(i)
                    val epId = ep.optString("id", "")
                    val epNum = ep.optString("episode_num", "1")
                    val title = ep.optString("title", "الحلقة $epNum")
                    val ext = ep.optString("container_extension", "mp4")
                    val epUrl = "$server/series/$user/$pass/$epId.$ext"
                    episodesList.add(MediaItemData(epId, "الموسم $seasonKey - $title", epUrl, ContentType.VOD, "الموسم $seasonKey", ""))
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    episodesList
}
