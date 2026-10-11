package com.stitten.stitteniptv.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.ui.navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {

    var logoVisible by remember { mutableStateOf(false) }
    var logoScale by remember { mutableStateOf(0.3f) }
    var logoAlpha by remember { mutableStateOf(0f) }
    var visibleLetters by remember { mutableStateOf(0) }
    var ipTvVisible by remember { mutableStateOf(false) }
    var dotsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(100)
        logoScale = 1f
        logoAlpha = 1f

        delay(400)
        for (i in 1..7) {
            visibleLetters = i
            delay(120)
        }

        delay(200)
        ipTvVisible = true

        delay(300)
        dotsVisible = true

        delay(1200)
        navController.navigate(Routes.DASHBOARD) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1F6FEB).copy(alpha = 0.25f),
                        Color(0xFF0D1117),
                        Color(0xFF05070D),
                    ),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // الشعار
            Text(
                text = "🎬",
                fontSize = 80.sp,
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha),
            )

            Spacer(Modifier.height(32.dp))

            // STTITEN حرفًا حرفًا
            Row {
                val letters = "STTITEN"
                letters.forEachIndexed { index, char ->
                    val isVisible = index < visibleLetters
                    Text(
                        text = char.toString(),
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .alpha(if (isVisible) 1f else 0f)
                            .scale(if (isVisible) 1f else 0.5f),
                    )
                    Spacer(Modifier.size(4.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // خط فاصل متوهج
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1F6FEB).copy(alpha = glowAlpha)),
            )

            Spacer(Modifier.height(12.dp))

            // IP TV
            Text(
                text = "I P   T V",
                color = Color(0xFF1F6FEB),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                modifier = Modifier.alpha(if (ipTvVisible) 1f else 0f),
            )

            Spacer(Modifier.height(64.dp))

            // نقاط التحميل
            if (dotsVisible) {
                LoadingDots()
            }
        }
    }
}

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alphas = List(3) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 700,
                    delayMillis = index * 200,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot$index",
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        alphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F6FEB).copy(alpha = alpha.value)),
            )
        }
    }
}
