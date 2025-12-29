package com.example.bananashield

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PreventionDetailsScreen(
    diseaseInfo: DiseaseInfo,
    onBack: () -> Unit
) {
    // ✅ Get status bar and navigation bar height
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density) / density.density

    // ✅ ADD BACK HANDLER
    BackHandler(enabled = true) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // ✅ Fixed Header (NOT scrollable)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column {
                Spacer(modifier = Modifier.height(statusBarHeight.dp + 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Preventive Measures",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Protect your crops from disease",
                            fontSize = 13.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }

        // ✅ ENTIRE CONTENT SCROLLABLE (including hero card)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ✅ Hero Card (NOW SCROLLABLE)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Prevention",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Prevention is Key",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Follow these measures to prevent disease spread and keep your plantation healthy.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Prevention Cards
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                diseaseInfo.preventiveMeasures.forEach { measure ->
                    ModernPreventionCard(
                        category = measure.category,
                        title = measure.title,
                        steps = measure.steps,
                        icon = getPreventionIcon(measure.icon)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // ✅ ADD NAVIGATION BAR PADDING HERE
            Spacer(modifier = Modifier.height((navigationBarHeight + 16).dp))
        }
    }
}

@Composable
fun ModernPreventionCard(
    category: String,
    title: String,
    steps: List<String>,
    icon: ImageVector
) {
    val backgroundColor = when (category) {
        "cultural" -> Color(0xFFFFF9C4)
        "water" -> Color(0xFFE3F2FD)
        "chemical" -> Color(0xFFFFEBEE)
        "resistant" -> Color(0xFFE8F5E9)
        "monitoring" -> Color(0xFFFFF3E0)
        "biosecurity" -> Color(0xFFF3E5F5)
        "nutrition" -> Color(0xFFE0F7FA)
        else -> Color(0xFFF5F5F5)
    }

    val iconColor = when (category) {
        "cultural" -> Color(0xFFF57F17)
        "water" -> Color(0xFF2196F3)
        "chemical" -> Color(0xFFEF5350)
        "resistant" -> Color(0xFF4CAF50)
        "monitoring" -> Color(0xFFFF9800)
        "biosecurity" -> Color(0xFF9C27B0)
        "nutrition" -> Color(0xFF00BCD4)
        else -> Color(0xFF757575)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Steps
            steps.forEach { step ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = step,
                        fontSize = 14.sp,
                        color = Color(0xFF424242),
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

fun getPreventionIcon(iconName: String): ImageVector {
    return when (iconName) {
        "plant" -> Icons.Default.Grass
        "water" -> Icons.Default.WaterDrop
        "spray" -> Icons.Default.Shower
        "variety" -> Icons.Default.Science
        "monitor" -> Icons.Default.Visibility
        "security" -> Icons.Default.Security
        "nutrition" -> Icons.Default.LocalFlorist
        else -> Icons.Default.Info
    }
}

