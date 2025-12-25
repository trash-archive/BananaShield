package com.example.bananashield

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@Composable
fun HistoryDetailScreen(
    scanHistory: ScanHistory,
    onBack: () -> Unit
) {
    var showTreatment by remember { mutableStateOf(false) }
    var showPrevention by remember { mutableStateOf(false) }

    // Convert ScanHistory to DiseaseInfo format so we can reuse Treatment/Prevention screens
    val diseaseInfo = remember(scanHistory) {
        DiseaseInfo(
            name = scanHistory.diseaseName,
            scientificName = scanHistory.scientificName,
            diseaseType = scanHistory.diseaseType,
            severity = scanHistory.severity,
            confidenceLevel = scanHistory.confidenceLevel,
            symptoms = scanHistory.symptoms,
            causes = scanHistory.causes,
            treatmentSteps = scanHistory.treatmentSteps.mapIndexed { index, stepMap ->
                TreatmentStep(
                    title = stepMap["title"] ?: "Treatment ${index + 1}",
                    description = stepMap["description"] ?: "",
                    icon = stepMap["icon"] ?: "alternative"
                )
            },
            preventiveMeasures = scanHistory.preventiveMeasures.mapIndexed { index, measureMap ->
                com.example.bananashield.PreventiveMeasure(
                    category = measureMap["category"] as? String ?: "biosecurity",
                    title = measureMap["title"] as? String ?: "Prevention ${index + 1}",
                    steps = (measureMap["steps"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                    icon = measureMap["icon"] as? String ?: "security"
                )
            },
            safetyNotes = scanHistory.safetyNotes.ifEmpty {
                listOf(
                    "Always wear protective equipment",
                    "Follow product label instructions carefully",
                    "Wash hands thoroughly after handling chemicals"
                )
            }
        )
    }

    when {
        showTreatment -> {
            TreatmentDetailsScreen(
                diseaseInfo = diseaseInfo,
                onBack = { showTreatment = false }
            )
        }
        showPrevention -> {
            PreventionDetailsScreen(
                diseaseInfo = diseaseInfo,
                onBack = { showPrevention = false }
            )
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                // Same gradient background as ResultsScreen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1B5E20),
                                    Color(0xFF2E7D32),
                                    Color(0xFF388E3C)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Scan Details",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Image
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            if (scanHistory.imageUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = scanHistory.imageUrl),
                                    contentDescription = "Scan image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.3f)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Main Disease Info Card – styled like ModernDiseaseCard but bound to history data
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors =
                                                    if (scanHistory.diseaseName.contains(
                                                            "Healthy",
                                                            ignoreCase = true
                                                        )
                                                    ) {
                                                        listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                                                    } else {
                                                        listOf(Color(0xFFFF9800), Color(0xFFFFB74D))
                                                    }
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector =
                                            if (scanHistory.diseaseName.contains(
                                                    "Healthy",
                                                    ignoreCase = true
                                                )
                                            ) {
                                                Icons.Default.CheckCircle
                                            } else {
                                                Icons.Default.WarningAmber
                                            },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = scanHistory.diseaseName,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = scanHistory.scientificName,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Metrics row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ModernMetricChip(
                                    label = "Confidence",
                                    value = scanHistory.confidenceLevel,
                                    icon = Icons.Default.Speed,
                                    gradientColors = when {
                                        scanHistory.confidence >= 0.80f ->
                                            listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                                        scanHistory.confidence >= 0.60f ->
                                            listOf(Color(0xFFFFD54F), Color(0xFFFFE082))
                                        else ->
                                            listOf(Color(0xFFFF5252), Color(0xFFFF8A80))
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                ModernMetricChip(
                                    label = "Severity",
                                    value = scanHistory.diseaseType,
                                    icon = Icons.Default.TrendingUp,
                                    gradientColors =
                                        if (scanHistory.diseaseName.contains(
                                                "Healthy",
                                                ignoreCase = true
                                            )
                                        ) {
                                            listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                                        } else {
                                            listOf(Color(0xFFFF5722), Color(0xFFFF7043))
                                        },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Date & Time",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = ScanHistoryHelper.formatTimestamp(scanHistory.timestamp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray
                                    )
                                }

                                if (scanHistory.location.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = scanHistory.location,
                                                fontSize = 11.sp,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Symptoms
                    if (scanHistory.symptoms.isNotEmpty()) {
                        ModernInfoSection(
                            title = "Identified Symptoms",
                            items = scanHistory.symptoms,
                            icon = Icons.Default.LocalHospital,
                            backgroundColor = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Causes
                    if (scanHistory.causes.isNotEmpty()) {
                        ModernInfoSection(
                            title = "Common Causes",
                            items = scanHistory.causes,
                            icon = Icons.Default.Info,
                            backgroundColor = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Treatment – clickable card
                    if (scanHistory.treatmentSteps.isNotEmpty()) {
                        ModernActionCard(
                            title = "Treatment Options",
                            subtitle = "${scanHistory.treatmentSteps.size} treatment methods available",
                            icon = Icons.Default.MedicalServices,
                            gradientColors = listOf(Color(0xFFE91E63), Color(0xFFF06292)),
                            onClick = { showTreatment = true }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Prevention – clickable card
                    if (scanHistory.preventiveMeasures.isNotEmpty()) {
                        ModernActionCard(
                            title = "Prevention Measures",
                            subtitle = "${scanHistory.preventiveMeasures.size} prevention strategies",
                            icon = Icons.Default.Security,
                            gradientColors = listOf(Color(0xFF2196F3), Color(0xFF64B5F6)),
                            onClick = { showPrevention = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
