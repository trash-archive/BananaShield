// ============================================
// FILE 1: ResultsScreen.kt
// ============================================
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

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
    var showImageViewer by remember { mutableStateOf(false) }
    var scanTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density) / density.density

    BackHandler(enabled = true) {
        onScanAgain()
    }

    LaunchedEffect(Unit) {
        scanTimestamp = System.currentTimeMillis()

        if (bitmap != null && classification != null && !hasSaved) {
            isSaving = true
            ScanHistoryHelper.saveScanResult(
                bitmap = bitmap,
                classification = classification,
                location = "",
                notes = if (classification.confidence < CONFIDENCE_MODERATE)
                    "Low confidence scan - may require verification" else "",
                onSuccess = { documentId ->
                    showSaveSuccess = true
                    isSaving = false
                    hasSaved = true

                    SystemNotificationHelper.showScanCompletedNotification(
                        context = context,
                        diseaseName = classification.diseaseInfo.name,
                        confidence = classification.confidence,
                        scanId = documentId
                    )

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
                    saveError = exception.message
                    isSaving = false
                }
            )
        }
    }

    if (isSaving) {
        SubtleLoadingDialog()
    }

    if (showImageViewer && bitmap != null) {
        ImageViewerDialog(
            bitmap = bitmap,
            onDismiss = { showImageViewer = false }
        )
    }

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

    // ✅ NEW: Use Column structure like HistoryDetailScreen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // ✅ ENTIRE CONTENT SCROLLABLE (including header)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ✅ Updated header with green theme matching HistoryDetailScreen
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
                        IconButton(onClick = onScanAgain) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF2E7D32) // ✅ Changed to green
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Analysis Results",
                                fontSize = 22.sp, // ✅ Increased from 20sp
                                fontWeight = FontWeight.Bold, // ✅ Changed to Bold
                                color = Color(0xFF1B5E20) // ✅ Changed to dark green
                            )
                            Text(
                                text = "Leaf health assessment",
                                fontSize = 13.sp,
                                color = Color(0xFF757575)
                            )
                        }

                        // ✅ Save indicator remains on the right
                        AnimatedVisibility(
                            visible = showSaveSuccess,
                            enter = fadeIn() + scaleIn()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Saved",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Saved",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (saveError != null) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Failed",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Error message if save failed
                    AnimatedVisibility(
                        visible = saveError != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Failed to save: Check internet",
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF5350)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SubtleImagePreview(
                bitmap = bitmap,
                onClick = { showImageViewer = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            classification?.let { result ->
                val info = result.diseaseInfo

                SubtleConfidenceCard(confidence = result.confidence)

                Spacer(modifier = Modifier.height(16.dp))

                SubtleDiseaseCard(
                    result = result,
                    info = info,
                    timestamp = scanTimestamp
                )

                Spacer(modifier = Modifier.height(16.dp))

                SubtleInfoSection(
                    title = "Identified Symptoms",
                    items = info.symptoms,
                    icon = Icons.Default.LocalHospital,
                    accentColor = Color(0xFF66BB6A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                SubtleInfoSection(
                    title = "Common Causes",
                    items = info.causes,
                    icon = Icons.Default.BugReport,
                    accentColor = Color(0xFF42A5F5)
                )

                Spacer(modifier = Modifier.height(16.dp))

                SubtleActionCard(
                    title = "Treatment Guide",
                    subtitle = if (result.label.contains("Healthy", ignoreCase = true))
                        "Maintain current practices"
                    else
                        "${info.treatmentSteps.size} treatment steps",
                    icon = Icons.Default.MedicalServices,
                    accentColor = Color(0xFFEC407A),
                    onClick = { showTreatmentDetails = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SubtleActionCard(
                    title = "Prevention Tips",
                    subtitle = "${info.preventiveMeasures.size} preventive measures",
                    icon = Icons.Default.Shield,
                    accentColor = Color(0xFF42A5F5),
                    onClick = { showPreventionDetails = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                SubtleScanAgainButton(onClick = onScanAgain)

                // ✅ Add navigation bar padding at bottom
                Spacer(modifier = Modifier.height((navigationBarHeight + 16).dp))
            }
        }
    }
}


@Composable
fun ImageViewerDialog(
    bitmap: Bitmap,
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

            Image(
                bitmap = bitmap.asImageBitmap(),
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
fun SubtleLoadingDialog() {
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier.size(180.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "loading")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .scale(scale),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(50.dp),
                        color = Color(0xFF66BB6A),
                        strokeWidth = 3.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Saving...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF424242)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Please wait",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }
        }
    }
}

@Composable
fun SubtleImagePreview(
    bitmap: Bitmap?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Scanned Leaf",
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
}

@Composable
fun SubtleConfidenceCard(confidence: Float) {
    val (backgroundColor, borderColor, icon, title, message) = when {
        confidence < CONFIDENCE_LOW -> Tuple5(
            Color(0xFFFFEBEE),
            Color(0xFFEF5350),
            Icons.Default.Error,
            "Very Low Confidence (${(confidence * 100).toInt()}%)",
            "Results may be unreliable. Consider rescanning."
        )
        confidence < CONFIDENCE_MODERATE -> Tuple5(
            Color(0xFFFFF3E0),
            Color(0xFFFF9800),
            Icons.Default.Info,
            "Moderate Confidence (${(confidence * 100).toInt()}%)",
            "Results are moderately reliable."
        )
        confidence < CONFIDENCE_HIGH -> Tuple5(
            Color(0xFFFFFDE7),
            Color(0xFFFBC02D),
            Icons.Default.CheckCircle,
            "Good Confidence (${(confidence * 100).toInt()}%)",
            "Results appear accurate."
        )
        else -> return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = borderColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = Color(0xFF616161),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

@Composable
fun SubtleDiseaseCard(
    result: Classification,
    info: DiseaseInfo,
    timestamp: Long
) {
    val isHealthy = result.label.contains("Healthy", ignoreCase = true)

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
                        text = info.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = info.scientificName,
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
                SubtleMetricChip(
                    label = "AI Confidence",
                    value = info.confidenceLevel,
                    icon = Icons.Default.Speed,
                    accentColor = when {
                        result.confidence >= CONFIDENCE_HIGH -> Color(0xFF66BB6A)
                        result.confidence >= CONFIDENCE_MODERATE -> Color(0xFFFBC02D)
                        else -> Color(0xFFEF5350)
                    },
                    modifier = Modifier.weight(1f)
                )

                SubtleMetricChip(
                    label = "Type",
                    value = info.diseaseType,
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
                    text = formatDetailedTimestamp(timestamp),
                    fontSize = 12.sp,
                    color = Color(0xFF616161),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SubtleMetricChip(
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
fun SubtleInfoSection(
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
fun SubtleActionCard(
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

@Composable
fun SubtleScanAgainButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2E7D32)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Scan Another Leaf",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

fun formatDetailedTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}