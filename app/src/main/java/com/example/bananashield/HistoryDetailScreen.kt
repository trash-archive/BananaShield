package com.example.bananashield

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryDetailScreen(
    scanHistory: ScanHistory,
    onBack: () -> Unit
) {
    var showTreatment by remember { mutableStateOf(false) }
    var showPrevention by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }

    // ✅ Get status bar and navigation bar height
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density) / density.density

    // ✅ ADD BACK HANDLER - goes back instead of exiting app
    BackHandler(enabled = true) {
        onBack()
    }

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
            // ✅ NEW: Use Column with background instead of Box
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F7FA))
            ) {
                // ✅ ENTIRE CONTENT SCROLLABLE (including header)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ✅ Header with status bar padding (NOW SCROLLABLE)
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

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Scan Details",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Text(
                                        text = "View complete analysis",
                                        fontSize = 13.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Image with click to view
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(horizontal = 20.dp)
                            .clickable { showImageViewer = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (scanHistory.imageUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = scanHistory.imageUrl),
                                    contentDescription = "Scan image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tap to enlarge",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HistorySubtleDiseaseCard(
                        scanHistory = scanHistory
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (scanHistory.symptoms.isNotEmpty()) {
                        HistorySubtleInfoSection(
                            title = "Identified Symptoms",
                            items = scanHistory.symptoms,
                            icon = Icons.Default.LocalHospital,
                            accentColor = Color(0xFF66BB6A)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (scanHistory.causes.isNotEmpty()) {
                        HistorySubtleInfoSection(
                            title = "Common Causes",
                            items = scanHistory.causes,
                            icon = Icons.Default.BugReport,
                            accentColor = Color(0xFF42A5F5)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (scanHistory.treatmentSteps.isNotEmpty()) {
                        HistorySubtleActionCard(
                            title = "Treatment Guide",
                            subtitle = "${scanHistory.treatmentSteps.size} treatment methods",
                            icon = Icons.Default.MedicalServices,
                            accentColor = Color(0xFFEC407A),
                            onClick = { showTreatment = true }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (scanHistory.preventiveMeasures.isNotEmpty()) {
                        HistorySubtleActionCard(
                            title = "Prevention Tips",
                            subtitle = "${scanHistory.preventiveMeasures.size} preventive measures",
                            icon = Icons.Default.Shield,
                            accentColor = Color(0xFF42A5F5),
                            onClick = { showPrevention = true }
                        )
                    }

                    // ✅ ADD NAVIGATION BAR PADDING HERE
                    Spacer(modifier = Modifier.height((navigationBarHeight + 16).dp))
                }
            }

            if (showImageViewer && scanHistory.imageUrl.isNotEmpty()) {
                HistoryImageViewerDialog(
                    imageUrl = scanHistory.imageUrl,
                    onDismiss = { showImageViewer = false }
                )
            }
        }
    }
}



@Composable
fun HistoryImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Full screen image
            Image(
                painter = rememberAsyncImagePainter(model = imageUrl),
                contentDescription = "Full size image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun HistorySubtleDiseaseCard(
    scanHistory: ScanHistory
) {
    val isHealthy = scanHistory.diseaseName.contains("Healthy", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (isHealthy)
                                Color(0xFFE8F5E9)
                            else
                                Color(0xFFFFF3E0),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHealthy)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (isHealthy)
                            Color(0xFF66BB6A)
                        else
                            Color(0xFFFF9800),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scanHistory.diseaseName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = scanHistory.scientificName,
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HistorySubtleMetricChip(
                    label = "AI Confidence",
                    value = scanHistory.confidenceLevel,
                    icon = Icons.Default.Speed,
                    accentColor = when {
                        scanHistory.confidence >= 0.80f -> Color(0xFF66BB6A)
                        scanHistory.confidence >= 0.60f -> Color(0xFFFBC02D)
                        else -> Color(0xFFEF5350)
                    },
                    modifier = Modifier.weight(1f)
                )

                HistorySubtleMetricChip(
                    label = "Type",
                    value = scanHistory.diseaseType,
                    icon = Icons.Default.Category,
                    accentColor = if (isHealthy)
                        Color(0xFF66BB6A)
                    else
                        Color(0xFFFF7043),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatHistoryTimestamp(scanHistory.timestamp),
                    fontSize = 12.sp,
                    color = Color(0xFF616161),
                    fontWeight = FontWeight.Medium
                )
            }

            if (scanHistory.location.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = scanHistory.location,
                        fontSize = 12.sp,
                        color = Color(0xFF616161)
                    )
                }
            }
        }
    }
}

@Composable
fun HistorySubtleMetricChip(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = accentColor.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HistorySubtleInfoSection(
    title: String,
    items: List<String>,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .offset(y = 7.dp)
                            .background(accentColor.copy(alpha = 0.6f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item,
                        fontSize = 13.sp,
                        color = Color(0xFF424242),
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HistorySubtleActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
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
                    .background(
                        accentColor.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View",
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

fun formatHistoryTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}