
// ============== تبديل المصادر ==============
fun switchSource(source: SourceEntity) {
    viewModelScope.launch {
        XtreamApi.setPreferredProtocol(prefs.useHttps)
        XtreamApi.setPreferredFormat(prefs.streamFormat)
        channelRepo.liteMode = prefs.liteModeEnabled

        withContext(Dispatchers.IO) { channelRepo.clearAll() }
        sourceMgr.setActive(source.id)

        _uiState.value = DashboardUiState(
            isLoading = true,
            loadingMessage = "🔐 جاري الاتصال بـ ${source.name}...",
            loadingProgress = 5,
            currentSourceName = source.name,
            currentSourceType = source.type
        )

        prefs.isLoggedIn = true
        prefs.loginType = source.type
        prefs.serverUrl = source.url
        prefs.username = source.username
        prefs.password = source.password
        prefs.m3uUrl = source.url

        when (source.type) {
            "XTREAM" -> {
                val valid = withContext(Dispatchers.IO) {
                    XtreamApi.validate(source.url, source.username, source.password)
                }
                if (!valid) {
                    _uiState.value = DashboardUiState(
                        error = "❌ فشل الاتصال بـ ${source.name}",
                        currentSourceName = source.name,
                        currentSourceType = source.type
                    )
                    return@launch
                }
                loadChannelsOnly(source.url, source.username, source.password)
            }
            "M3U" -> loadM3uSource(source)
        }
    }
}

private suspend fun loadM3uSource(source: SourceEntity) {
    _uiState.value = _uiState.value.copy(
        loadingMessage = "📥 جاري تحميل M3U...",
        loadingProgress = 20
    )

    val channels = if (source.url.startsWith("content://")) {
        emptyList()
    } else {
        withContext(Dispatchers.IO) { M3uParser.loadFromUrl(source.url) }
    }

    if (channels.isEmpty()) {
        _uiState.value = DashboardUiState(
            error = "❌ فشل تحميل M3U: ${source.name}",
            currentSourceName = source.name,
            currentSourceType = source.type
        )
        return
    }

    val limited = channels.take(getChannelLimit())
    withContext(Dispatchers.IO) { channelRepo.saveChannels(limited) }

    _uiState.value = _uiState.value.copy(
        channelsCount = limited.size,
        channelsLoaded = true,
        isLoading = false,
        loadingProgress = 100,
        loadingMessage = "✅ ${limited.size} قناة",
        dataVersion = _uiState.value.dataVersion + 1
    )
}

fun loginM3u(url: String, content: String? = null, onDone: (Boolean) -> Unit) {
    viewModelScope.launch {
        _uiState.value = DashboardUiState(
            isLoading = true,
            loadingMessage = "📥 جاري تحميل القائمة...",
            loadingProgress = 20
        )

        val list = if (content != null) {
            M3uParser.loadFromContent(content)
        } else {
            M3uParser.loadFromUrl(url)
        }

        if (list.isEmpty()) {
            _uiState.value = DashboardUiState(error = "❌ لا توجد قنوات")
            onDone(false)
        } else {
            prefs.isLoggedIn = true
            prefs.loginType = "M3U"
            prefs.m3uUrl = url

            val limited = list.take(getChannelLimit())
            withContext(Dispatchers.IO) {
                channelRepo.clearAll()
                channelRepo.saveChannels(limited)
            }

            _uiState.value = DashboardUiState(
                channelsCount = limited.size,
                channelsLoaded = true,
                loadingProgress = 100,
                dataVersion = 1
            )
            onDone(true)
        }
    }
}
