package com.stitten.stitteniptv.ui.vod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes

@Composable
fun MovieDetailsScreen(
    navController: NavHostController,
    viewModel: MovieDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        when {
            state.isLoading -> LoadingView()

            state.errorMessage != null -> ErrorView(
                message = state.errorMessage!!,
                onRetry = viewModel::retry,
                onBack = { navController.popBackStack() },
            )

            state.movie != null -> MovieDetailsContent(
                movie = state.movie!!,
                onPlay = {
                    navController.navigate(
                        Routes.player(state.movie!!.url, state.movie!!.name)
                    )
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun MovieDetailsContent(
    movie: MovieEntity,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero section ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            // صورة الخلفية (backdrop أو poster)
            AsyncImage(
                model = movie.backdrop ?: movie.poster,
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // تدرج داكن
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.6f),
                                Color(0xFF0D1117).copy(alpha = 1f),
                            )
                        )
                    )
            )

            // المحتوى أسفل البانر
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                Text(
                    text = movie.name,
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val infoLine = buildList {
                    if (!movie.year.isNullOrBlank()) add(movie.year)
                    if (!movie.rating.isNullOrBlank()) add("⭐ ${movie.rating}")
                    if (!movie.duration.isNullOrBlank()) add(movie.duration)
                    if (!movie.genre.isNullOrBlank()) add(movie.genre)
                }.joinToString("  ·  ")

                if (infoLine.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = infoLine,
                        color = Color(0xFFD0D7DE),
                        fontSize = 15.sp,
                    )
                }

                Spacer(Modifier.height(20.dp))

                // أزرار
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayButton(onClick = onPlay)
                    BackButton(onClick = onBack)
                }
            }
        }

        // ── القصة ──
        if (!movie.plot.isNullOrBlank()) {
            Section(title = "القصة") {
                Text(
                    text = movie.plot,
                    color = Color(0xFFD0D7DE),
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                )
            }
        }

        // ── التفاصيل ──
        val details = buildList {
            if (!movie.director.isNullOrBlank()) add("المخرج" to movie.director)
            if (!movie.cast.isNullOrBlank()) add("الممثلون" to movie.cast)
            if (!movie.genre.isNullOrBlank()) add("التصنيف" to movie.genre)
            if (!movie.category.isNullOrBlank()) add("الفئة" to movie.category)
        }

        if (details.isNotEmpty()) {
            Section(title = "معلومات") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    details.forEach { (label, value) ->
                        Row {
                            Text(
                                text = "$label: ",
                                color = Color(0xFF8B949E),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = value,
                                color = Color(0xFFD0D7DE),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
//  مكونات مساعدة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit) {
    Text(
        text = "▶  تشغيل",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1F6FEB))
            .padding(horizontal = 28.dp, vertical = 12.dp)
    )
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Text(
        text = "← رجوع",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF30363D))
            .padding(horizontal = 28.dp, vertical = 12.dp)
    )
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF1F6FEB))
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("⚠️", fontSize = 48.sp)
            Text(message, color = Color.White, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlayButton(onClick = onRetry)
                BackButton(onClick = onBack)
            }
        }
    }
}
