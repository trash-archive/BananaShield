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

    // Convert ScanHistory to DiseaseInfo format
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
                // Using the full package path to avoid confusion
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2E7D32))
                    .verticalScroll(rememberScrollState())
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
                        text = "Scan Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Disease Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (scanHistory.diseaseName.contains("Healthy", ignoreCase = true))
                                            Color(0xFFE8F5E9)
                                        else
                                            Color(0xFFFFF3E0)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (scanHistory.diseaseName.contains("Healthy", ignoreCase = true))
                                        Icons.Default.CheckCircle
                                    else
                                        Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (scanHistory.diseaseName.contains("Healthy", ignoreCase = true))
                                        Color(0xFF4CAF50)
                                    else
                                        Color(0xFFFF9800),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scanHistory.diseaseName,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = scanHistory.scientificName,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricChip(
                                label = "Confidence",
                                value = scanHistory.confidenceLevel,
                                color = Color(0xFFFFD54F),
                                modifier = Modifier.weight(1f)
                            )
                            MetricChip(
                                label = scanHistory.severity,
                                value = scanHistory.diseaseType,
                                color = if (scanHistory.diseaseName.contains("Healthy", ignoreCase = true))
                                    Color(0xFF4CAF50)
                                else
                                    Color(0xFFFF5722),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Date Time",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = ScanHistoryHelper.formatTimestamp(scanHistory.timestamp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )

                        if (scanHistory.location.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = scanHistory.location,
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Symptoms
                if (scanHistory.symptoms.isNotEmpty()) {
                    InfoCard(
                        title = "Identified Symptoms",
                        items = scanHistory.symptoms,
                        icon = Icons.Default.LocalHospital
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Causes
                if (scanHistory.causes.isNotEmpty()) {
                    InfoCard(
                        title = "Common Causes",
                        items = scanHistory.causes,
                        icon = Icons.Default.Info
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Treatment Steps - Clickable
                if (scanHistory.treatmentSteps.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { showTreatment = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFCE4EC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Treatment Options",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "${scanHistory.treatmentSteps.size} treatment methods available",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View treatment",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Prevention Measures - Clickable
                if (scanHistory.preventiveMeasures.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { showPrevention = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE3F2FD)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Prevention Measures",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "${scanHistory.preventiveMeasures.size} prevention strategies",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View prevention",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
