package com.example.bananashield

import android.graphics.Bitmap
import android.util.Log
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Confidence thresholds based on medical AI research
private const val CONFIDENCE_HIGH = 0.80f      // 80%+ = High confidence
private const val CONFIDENCE_MODERATE = 0.60f  // 60-79% = Moderate confidence
private const val CONFIDENCE_LOW = 0.40f       // 40-59% = Low confidence (show strong warning)
// Below 40% = Very unreliable

@Composable
fun ResultsScreen(
    bitmap: Bitmap?,
    classification: Classification?,
    onScanAgain: () -> Unit
) {
    var showTreatmentDetails by remember { mutableStateOf(false) }
    var showPreventionDetails by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var hasSaved by remember { mutableStateOf(false) }

    // Auto-save scan result to Firebase (saves all results regardless of confidence)
    LaunchedEffect(Unit) {
        if (bitmap != null && classification != null && !hasSaved) {
            Log.d("ResultsScreen", "🔄 Attempting to save scan...")
            isSaving = true

            ScanHistoryHelper.saveScanResult(
                bitmap = bitmap,
                classification = classification,
                location = "Cebu City, Philippines",
                notes = if (classification.confidence < CONFIDENCE_MODERATE)
                    "Low confidence scan - may require verification" else "",
                onSuccess = { documentId ->
                    Log.d("ResultsScreen", "✅ Saved successfully! Doc ID: $documentId")
                    showSaveSuccess = true
                    isSaving = false
                    hasSaved = true
                },
                onFailure = { exception ->
                    Log.e("ResultsScreen", "❌ Save failed: ${exception.message}")
                    saveError = exception.message
                    isSaving = false
                }
            )
        }
    }

    // Loading Dialog
    if (isSaving) {
        Dialog(onDismissRequest = { }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = Color(0xFF4CAF50),
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Saving to Database...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please wait",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Show detail screens if requested
    if (showTreatmentDetails && classification != null) {
        TreatmentDetailsScreen(
            diseaseInfo = classification.diseaseInfo,
            onBack = { showTreatmentDetails = false }
        )
        return
    }

    if (showPreventionDetails && classification != null) {
        PreventionDetailsScreen(
            diseaseInfo = classification.diseaseInfo,
            onBack = { showPreventionDetails = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
            .verticalScroll(rememberScrollState())
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onScanAgain) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Scan Results",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Save Status Indicator
            when {
                showSaveSuccess -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Saved",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Saved",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
                saveError != null -> {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Save failed",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Show error message if save failed
        if (saveError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Failed to save: Check internet connection",
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }

        // Image Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Captured Banana Leaf",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        classification?.let { result ->
            val info = result.diseaseInfo

            // Confidence Warnings (tiered system)
            when {
                // Very Low Confidence (<40%) - Strong warning
                result.confidence < CONFIDENCE_LOW -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Very Low Confidence (${(result.confidence * 100).toInt()}%)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This result may be unreliable. Please rescan with:\n• Better lighting\n• Closer view of affected area\n• Clear focus on leaf surface",
                                    fontSize = 12.sp,
                                    color = Color(0xFFD32F2F),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // Low-Moderate Confidence (40-59%) - Moderate warning
                result.confidence < CONFIDENCE_MODERATE -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Moderate Confidence (${(result.confidence * 100).toInt()}%)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Consider rescanning for better accuracy or consult with an agricultural expert to verify this diagnosis.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // Good Confidence (60-79%) - Light info badge
                result.confidence < CONFIDENCE_HIGH -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFF57F17),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Good confidence (${(result.confidence * 100).toInt()}%). Result appears reliable.",
                                fontSize = 12.sp,
                                color = Color(0xFFF57F17),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // High Confidence (≥80%) - No warning needed
            }

            // Main Info Card
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
                                    if (result.label.contains("Healthy", ignoreCase = true))
                                        Color(0xFFE8F5E9)
                                    else
                                        Color(0xFFFFF3E0)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (result.label.contains("Healthy", ignoreCase = true))
                                    Icons.Default.CheckCircle
                                else
                                    Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (result.label.contains("Healthy", ignoreCase = true))
                                    Color(0xFF4CAF50)
                                else
                                    Color(0xFFFF9800),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = info.scientificName,
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
                            value = info.confidenceLevel,
                            color = when {
                                result.confidence >= CONFIDENCE_HIGH -> Color(0xFF4CAF50)      // Green
                                result.confidence >= CONFIDENCE_MODERATE -> Color(0xFFFFD54F) // Yellow
                                result.confidence >= CONFIDENCE_LOW -> Color(0xFFFF9800)      // Orange
                                else -> Color(0xFFFF5252)                                      // Red
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MetricChip(
                            label = info.severity,
                            value = info.diseaseType,
                            color = if (result.label.contains("Healthy", ignoreCase = true))
                                Color(0xFF4CAF50)
                            else
                                Color(0xFFFF5722),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Date & Time",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = ScanHistoryHelper.formatTimestamp(System.currentTimeMillis()),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Symptoms Card
            InfoCard(
                title = "Identified Symptoms",
                items = info.symptoms,
                icon = Icons.Default.LocalHospital
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Causes Card
            InfoCard(
                title = "Common Causes",
                items = info.causes,
                icon = Icons.Default.Info
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Severity Warning (only for diseases)
            if (!result.label.contains("Healthy", ignoreCase = true)) {
                SeverityCard(severity = info.severity)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Treatment Action Card
            ActionCard(
                title = "Recommended Treatment",
                subtitle = if (result.label.contains("Healthy", ignoreCase = true))
                    "Maintain current practices"
                else
                    "${info.treatmentSteps.size} treatment options available",
                icon = Icons.Default.MedicalServices,
                backgroundColor = Color(0xFFFCE4EC),
                iconColor = Color(0xFFE91E63),
                onClick = { showTreatmentDetails = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Prevention Action Card
            ActionCard(
                title = "Prevention Measures",
                subtitle = "${info.preventiveMeasures.size} preventive strategies",
                icon = Icons.Default.Security,
                backgroundColor = Color(0xFFE3F2FD),
                iconColor = Color(0xFF2196F3),
                onClick = { showPreventionDetails = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Scan Again Button
            Button(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    tint = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan Again",
                    color = Color(0xFF1B5E20),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MetricChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.9f)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun InfoCard(title: String, items: List<String>, icon: ImageVector) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            items.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "•",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = item,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SeverityCard(severity: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = severity,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Immediate treatment recommended",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
