package com.example.bananashield

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    val context = LocalContext.current

    // Auto-dismiss after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    // Pulsing animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B5E20),
                        Color(0xFF2E7D32),
                        Color(0xFF388E3C)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo from mipmap using drawable
            val drawable = remember {
                ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            }

            drawable?.let { d ->
                val bitmap = remember {
                    val width = d.intrinsicWidth.takeIf { it > 0 } ?: 512
                    val height = d.intrinsicHeight.takeIf { it > 0 } ?: 512
                    android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888).also { bitmap ->
                        val canvas = android.graphics.Canvas(bitmap)
                        d.setBounds(0, 0, canvas.width, canvas.height)
                        d.draw(canvas)
                    }
                }

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "BananaShield Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BananaShield",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Papasara tawn mi ninyo sir malooy mo " +
                        "\uD83E\uDD79\n",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
