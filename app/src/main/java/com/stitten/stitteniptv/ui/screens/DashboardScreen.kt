// ═══════════════════════════════════════════════════════════════
//  ✅ الهيكل: Sidebar عمودي واسع + المحتوى يميناً
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onMovieClick: (MovieEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit,
    onChannelClick: (ChannelEntity) -> Unit,
    onContinueClick: (WatchHistoryEntity) -> Unit,
    onRemoveHistory: (WatchHistoryEntity) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLiveClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onSeriesListClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {

        // ═══════ الشريط الجانبي (يسار) ═══════
        SideNavBar(
            onMoviesClick = onMoviesClick,
            onSeriesListClick = onSeriesListClick,
            onLiveClick = onLiveClick,
            onFavoritesClick = onFavoritesClick,
            onSearchClick = onSearchClick,
            onRefreshClick = onRefreshClick,
            onSettingsClick = onSettingsClick,
        )

        // ═══════ المحتوى (يمين) ═══════
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            state.featuredMovie?.let { movie ->
                item(key = "hero") {
                    HeroBanner(
                        title = movie.name,
                        posterUrl = movie.poster,
                        backdropUrl = movie.backdrop,
                        plot = movie.plot,
                        rating = movie.rating,
                        year = movie.year,
                        subtitle = movie.genre ?: "فيلم",
                        onPlay = { onMovieClick(movie) },
                        onDetails = { onMovieClick(movie) },
                    )
                }
            }

            if (state.continueWatching.isNotEmpty()) {
                item(key = "continue") {
                    Spacer(Modifier.height(16.dp))
                    ContinueWatchingSection(
                        items = state.continueWatching,
                        onItemClick = onContinueClick,
                        onRemoveItem = onRemoveHistory,
                    )
                }
            }

            if (state.latestMovies.isNotEmpty()) {
                item(key = "movies") {
                    Spacer(Modifier.height(20.dp))
                    ContentRow(
                        title = "أحدث الأفلام",
                        icon = AppIcons.Movies,
                        items = state.latestMovies,
                        key = { it.id },
                    ) { movie ->
                        PosterCard(
                            title = movie.name,
                            imageUrl = movie.poster,
                            subtitle = buildSubtitle(movie.rating, movie.year),
                            onClick = { onMovieClick(movie) },
                        )
                    }
                }
            }

            if (state.topRatedMovies.isNotEmpty()) {
                item(key = "top_rated") {
                    Spacer(Modifier.height(20.dp))
                    ContentRow(
                        title = "الأعلى تقييمًا",
                        icon = AppIcons.Trending,
                        items = state.topRatedMovies,
                        key = { "top_${it.id}" },
                    ) { movie ->
                        PosterCard(
                            title = movie.name,
                            imageUrl = movie.poster,
                            subtitle = buildSubtitle(movie.rating, movie.year),
                            onClick = { onMovieClick(movie) },
                        )
                    }
                }
            }

            if (state.allSeries.isNotEmpty()) {
                item(key = "series") {
                    Spacer(Modifier.height(20.dp))
                    ContentRow(
                        title = "المسلسلات",
                        icon = AppIcons.Series,
                        items = state.allSeries,
                        key = { it.id },
                    ) { series ->
                        PosterCard(
                            title = series.name,
                            imageUrl = series.poster,
                            subtitle = buildSubtitle(series.rating, series.year),
                            onClick = { onSeriesClick(series) },
                        )
                    }
                }
            }

            if (state.liveChannels.isNotEmpty()) {
                item(key = "channels") {
                    Spacer(Modifier.height(20.dp))
                    ContentRow(
                        title = "قنوات مباشرة",
                        icon = AppIcons.LiveTv,
                        items = state.liveChannels,
                        key = { it.id },
                    ) { channel ->
                        PosterCard(
                            title = channel.name,
                            imageUrl = channel.logo,
                            subtitle = channel.groupName.takeIf { it.isNotBlank() },
                            isChannel = true,
                            onClick = { onChannelClick(channel) },
                        )
                    }
                }
            }

            if (
                state.featuredMovie == null &&
                state.latestMovies.isEmpty() &&
                state.topRatedMovies.isEmpty() &&
                state.allSeries.isEmpty() &&
                state.liveChannels.isEmpty() &&
                state.continueWatching.isEmpty()
            ) {
                item(key = "empty") { EmptyStateView() }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ✅ الشريط الجانبي المحسّن (أيقونات كبيرة + نصوص واضحة)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SideNavBar(
    onMoviesClick: () -> Unit,
    onSeriesListClick: () -> Unit,
    onLiveClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(140.dp)                                  // ✅ أعرض من السابق
            .background(Color(0xFF161B22))
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ═══════ الشعار في الأعلى ═══════
        Icon(
            imageVector = AppIcons.Movies,
            contentDescription = null,
            tint = Color(0xFF1F6FEB),
            modifier = Modifier.size(36.dp),                // ✅ أكبر
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "STTITEN",
            color = Color(0xFF1F6FEB),
            fontSize = 13.sp,                               // ✅ أكبر
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        // ═══════ عناصر التنقل ═══════
        SideNavButton(
            icon = Icons.Default.Movie,
            label = "أفلام",
            onClick = onMoviesClick,
        )
        SideNavButton(
            icon = Icons.Default.Tv,
            label = "مسلسلات",
            onClick = onSeriesListClick,
        )
        SideNavButton(
            icon = Icons.Default.LiveTv,
            label = "مباشر",
            onClick = onLiveClick,
        )
        SideNavButton(
            icon = Icons.Default.Favorite,
            label = "المفضلة",
            tint = Color(0xFFF85149),
            onClick = onFavoritesClick,
        )
        SideNavButton(
            icon = Icons.Default.Search,
            label = "بحث",
            onClick = onSearchClick,
        )
        SideNavButton(
            icon = Icons.Default.Refresh,
            label = "مزامنة",
            tint = Color(0xFF66BB6A),
            onClick = onRefreshClick,
        )
        SideNavButton(
            icon = Icons.Default.Settings,
            label = "إعدادات",
            onClick = onSettingsClick,
        )
    }
}

/**
 * زر واحد في الشريط الجانبي: أيقونة كبيرة + كتابة واضحة تحتها
 */
@Composable
private fun SideNavButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    Column(
        modifier = Modifier
            .width(124.dp)                                  // ✅ أعرض
            .tvFocusable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(34.dp),                // ✅ أيقونة أكبر بكثير (كانت 26)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 14.sp,                               // ✅ خط أكبر (كان 11)
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
