package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.ui.navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        delay(2000)
        val prefs = PrefsManager(ctx)
        val route = if (prefs.isLoggedIn) Routes.DASHBOARD else Routes.LOGIN
        navController.navigate(route) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "STTITEN IP TV",
                color = Color(0xFF1F6FEB),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "تطبيق بث القنوات والأفلام للأندرويد تي في",
                color = Color.White,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(color = Color(0xFF1F6FEB))
        }
    }
}
