package com.example.bananashield

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PreventionDetailsScreen(
    diseaseInfo: DiseaseInfo,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Preventive Measures",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Subtitle
        Text(
            text = "Follow these measures to prevent disease spread",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 60.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            diseaseInfo.preventiveMeasures.forEach { measure ->
                PreventionCard(
                    category = measure.category,
                    title = measure.title,
                    steps = measure.steps,
                    icon = getPreventionIcon(measure.icon)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PreventionCard(
    category: String,
    title: String,
    steps: List<String>,
    icon: ImageVector
) {
    val backgroundColor = when (category) {
        "cultural" -> Color(0xFFFFF9C4)
        "water" -> Color(0xFFB3E5FC)
        "chemical" -> Color(0xFFF8BBD0)
        "resistant" -> Color(0xFFDCEDC8)
        "monitoring" -> Color(0xFFFFE0B2)
        "biosecurity" -> Color(0xFFD1C4E9)
        "nutrition" -> Color(0xFFB2DFDB)
        else -> Color(0xFFE0E0E0)
    }

    val iconColor = when (category) {
        "cultural" -> Color(0xFFF57F17)
        "water" -> Color(0xFF0277BD)
        "chemical" -> Color(0xFFC2185B)
        "resistant" -> Color(0xFF558B2F)
        "monitoring" -> Color(0xFFE65100)
        "biosecurity" -> Color(0xFF512DA8)
        "nutrition" -> Color(0xFF00695C)
        else -> Color.Gray
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
                    color = Color(0xFF1B5E20)
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
                        color = Color.DarkGray,
                        lineHeight = 20.sp,
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
