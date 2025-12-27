package com.example.bananashield

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// Confidence thresholds
private const val CONFIDENCE_HIGH = 0.80f
private const val CONFIDENCE_MODERATE = 0.60f
private const val CONFIDENCE_LOW = 0.40f

@Composable
fun ResultsScreen(
    bitmap: Bitmap?,
    classification: Classification?,
    onScanAgain: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    var showTreatmentDetails by remember { mutableStateOf(false) }
    var showPreventionDetails by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var hasSaved by remember { mutableStateOf(false) }

    // Handle back button press
    BackHandler(enabled = true) {
        onScanAgain()
    }

    // Auto-save scan result + system notification
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

                    // ✅ Show system notification in status bar
                    SystemNotificationHelper.showScanCompletedNotification(
                        context = context,
                        diseaseName = classification.diseaseInfo.name,
                        confidence = classification.confidence,
                        scanId = documentId
                    )

                    // Also create in-app notification
                    currentUser?.uid?.let { userId ->
                        NotificationHelper.notifyScanComplete(
                            userId = userId,
                            scanId = documentId,
                            diseaseName = classification.diseaseInfo.name,
                            confidence = classification.confidence
                        )
                    }
                },
                onFailure = { exception ->
                    Log.e("ResultsScreen", "❌ Save failed: ${exception.message}")
                    saveError = exception.message
                    isSaving = false
                }
            )
        }
    }

    // Modern Loading Dialog
    if (isSaving) {
        ModernLoadingDialog()
    }

    // Show detail screens
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Background gradient
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
            // Modern Header
            ModernHeader(
                onBack = onScanAgain,
                showSaveSuccess = showSaveSuccess,
                saveError = saveError
            )

            // Image Preview with better styling
            ModernImagePreview(bitmap = bitmap)

            Spacer(modifier = Modifier.height(20.dp))

            classification?.let { result ->
                val info = result.diseaseInfo

                // Confidence Warning Cards
                ConfidenceWarningCard(confidence = result.confidence)

                Spacer(modifier = Modifier.height(16.dp))

                // Main Disease Info Card
                ModernDiseaseCard(
                    result = result,
                    info = info
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Symptoms Section
                ModernInfoSection(
                    title = "Identified Symptoms",
                    items = info.symptoms,
                    icon = Icons.Default.LocalHospital,
                    backgroundColor = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Causes Section
                ModernInfoSection(
                    title = "Common Causes",
                    items = info.causes,
                    icon = Icons.Default.BugReport,
                    backgroundColor = Color(0xFF2196F3)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Cards
                ModernActionCard(
                    title = "Treatment Guide",
                    subtitle = if (result.label.contains("Healthy", ignoreCase = true))
                        "Keep maintaining good practices"
                    else
                        "${info.treatmentSteps.size} step treatment plan",
                    icon = Icons.Default.MedicalServices,
                    gradientColors = listOf(Color(0xFFE91E63), Color(0xFFF06292)),
                    onClick = { showTreatmentDetails = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernActionCard(
                    title = "Prevention Tips",
                    subtitle = "${info.preventiveMeasures.size} preventive measures",
                    icon = Icons.Default.Shield,
                    gradientColors = listOf(Color(0xFF2196F3), Color(0xFF64B5F6)),
                    onClick = { showPreventionDetails = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Scan Again Button
                ModernScanAgainButton(onClick = onScanAgain)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ModernLoadingDialog() {
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier.size(200.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "loading")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(50.dp),
                        color = Color(0xFF4CAF50),
                        strokeWidth = 4.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Saving Result",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Please wait...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ModernHeader(
    onBack: () -> Unit,
    showSaveSuccess: Boolean,
    saveError: String?
) {
    Column {
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
                text = "Scan Results",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            AnimatedVisibility(
                visible = showSaveSuccess,
                enter = fadeIn() + scaleIn()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF4CAF50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Saved",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Saved",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (saveError != null) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Failed",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = saveError != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Failed to save: Check internet",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ModernImagePreview(bitmap: Bitmap?) {
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
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Scanned Leaf",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
}

@Composable
fun ConfidenceWarningCard(confidence: Float) {
    val (backgroundColor, iconColor, icon, title, message) = when {
        confidence < CONFIDENCE_LOW -> {
            Tuple5(
                Color(0xFFFFCDD2),
                Color(0xFFD32F2F),
                Icons.Default.Error,
                "Very Low Confidence (${(confidence * 100).toInt()}%)",
                "Result may be unreliable. Rescan with better lighting and focus."
            )
        }
        confidence < CONFIDENCE_MODERATE -> {
            Tuple5(
                Color(0xFFFFF3E0),
                Color(0xFFFF9800),
                Icons.Default.Info,
                "Moderate Confidence (${(confidence * 100).toInt()}%)",
                "Consider rescanning or consulting an expert for verification."
            )
        }
        confidence < CONFIDENCE_HIGH -> {
            Tuple5(
                Color(0xFFFFF9C4),
                Color(0xFFF57F17),
                Icons.Default.CheckCircle,
                "Good Confidence (${(confidence * 100).toInt()}%)",
                "Result appears reliable and accurate."
            )
        }
        else -> return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = iconColor.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

@Composable
fun ModernDiseaseCard(result: Classification, info: DiseaseInfo) {
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
                                colors = if (result.label.contains("Healthy", ignoreCase = true))
                                    listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                                else
                                    listOf(Color(0xFFFF9800), Color(0xFFFFB74D))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (result.label.contains("Healthy", ignoreCase = true))
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = info.scientificName,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernMetricChip(
                    label = "Confidence",
                    value = info.confidenceLevel,
                    icon = Icons.Default.Speed,
                    gradientColors = when {
                        result.confidence >= CONFIDENCE_HIGH -> listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                        result.confidence >= CONFIDENCE_MODERATE -> listOf(Color(0xFFFFD54F), Color(0xFFFFE082))
                        else -> listOf(Color(0xFFFF5252), Color(0xFFFF8A80))
                    },
                    modifier = Modifier.weight(1f)
                )

                ModernMetricChip(
                    label = "Severity",
                    value = info.diseaseType,
                    icon = Icons.Default.TrendingUp,
                    gradientColors = if (result.label.contains("Healthy", ignoreCase = true))
                        listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                    else
                        listOf(Color(0xFFFF5722), Color(0xFFFF7043)),
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
                        text = ScanHistoryHelper.formatTimestamp(System.currentTimeMillis()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                            text = "Cebu City",
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

@Composable
fun ModernMetricChip(
    label: String,
    value: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(colors = gradientColors.map { it.copy(alpha = 0.15f) })
                )
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = gradientColors[0],
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = gradientColors[0]
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ModernInfoSection(
    title: String,
    items: List<String>,
    icon: ImageVector,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.3f)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .offset(y = 7.dp)
                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModernActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(colors = gradientColors),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Surface(
                shape = CircleShape,
                color = gradientColors[0].copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View",
                        tint = gradientColors[0],
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernScanAgainButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFD54F), Color(0xFFFFE082))
                    )
                )
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Scan Another Leaf",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }
        }
    }
}
