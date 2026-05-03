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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.res.painterResource
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
    onScanAgain: () -> Unit,
    onAbortBBTV: () -> Unit = onScanAgain
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    var showTreatmentDetails by remember { mutableStateOf(false) }
    var showPreventionDetails by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var hasSaved by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var scanTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // BBTV questionnaire state
    val isBBTV = classification?.label?.contains("Bunchy Top", ignoreCase = true) == true
    var showBBTVQuestionnaire by remember { mutableStateOf(isBBTV) }
    var bbtvResult by remember { mutableStateOf<BBTVQuestionnaireResult?>(null) }
    var finalClassification by remember { mutableStateOf(classification) }

    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density) / density.density

    BackHandler(enabled = true) {
        onScanAgain()
    }

    // Show BBTV questionnaire before results
    if (showBBTVQuestionnaire) {
        BBTVQuestionnaireScreen(
            onComplete = { result ->
                bbtvResult = result
                finalClassification = classification?.copy(
                    bbtvVerdict = result.verdict,
                    bbtvScore = result.score,
                    bbtvStreakAnswer = result.streakAnswer,
                    bbtvTimelineAnswer = result.timelineAnswer,
                    bbtvSpreadAnswer = result.spreadAnswer,
                    bbtvAphidAnswer = result.aphidAnswer
                )
                showBBTVQuestionnaire = false
            },
            onAbort = onAbortBBTV
        )
        return
    }

    LaunchedEffect(Unit) {
        scanTimestamp = System.currentTimeMillis()

        if (bitmap != null && finalClassification != null && !hasSaved) {
            ScanHistoryHelper.saveScanResult(
                bitmap = bitmap,
                classification = finalClassification!!,
                location = "",
                notes = if (finalClassification!!.confidence < CONFIDENCE_MODERATE)
                    "Low confidence scan - may require verification" else "",
                onSuccess = { documentId ->
                    showSaveSuccess = true
                    hasSaved = true

                    SystemNotificationHelper.showScanCompletedNotification(
                        context = context,
                        diseaseName = finalClassification!!.diseaseInfo.name,
                        confidence = finalClassification!!.confidence,
                        scanId = documentId
                    )

                    currentUser?.uid?.let { userId ->
                        NotificationHelper.notifyScanComplete(
                            userId = userId,
                            scanId = documentId,
                            diseaseName = finalClassification!!.diseaseInfo.name,
                            confidence = finalClassification!!.confidence
                        )
                    }
                },
                onFailure = { exception ->
                    saveError = exception.message
                }
            )
        }
    }

    if (showImageViewer && bitmap != null) {
        ImageViewerDialog(
            bitmap = bitmap,
            onDismiss = { showImageViewer = false }
        )
    }

    if (showTreatmentDetails && finalClassification != null) {
        TreatmentDetailsScreen(
            diseaseInfo = finalClassification!!.diseaseInfo,
            onBack = { showTreatmentDetails = false }
        )
        return
    }

    if (showPreventionDetails && finalClassification != null) {
        PreventionDetailsScreen(
            diseaseInfo = finalClassification!!.diseaseInfo,
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
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            if (finalClassification?.plantLabel?.isNotEmpty() == true) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = finalClassification!!.plantLabel,
                                        fontSize = 13.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = "Leaf health assessment",
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            }
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

            Spacer(modifier = Modifier.height(12.dp))

            classification?.let { label -> SampleImagesCard(label = label.label) }

            Spacer(modifier = Modifier.height(20.dp))

            classification?.let { result ->
                val info = result.diseaseInfo

                SubtleConfidenceCard(confidence = result.confidence)

                Spacer(modifier = Modifier.height(16.dp))

                // BBTV verdict banner
                if (isBBTV && bbtvResult != null) {
                    BBTVVerdictBanner(bbtvResult = bbtvResult!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Class breakdown for transparency
                if (result.allConfidences.isNotEmpty()) {
                    ClassBreakdownCard(allConfidences = result.allConfidences)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SubtleDiseaseCard(
                    result = result,
                    info = info,
                    timestamp = scanTimestamp,
                    bbtvVerdict = finalClassification?.bbtvVerdict
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
                    contentScale = ContentScale.Fit
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
    timestamp: Long,
    bbtvVerdict: BBTVVerdict? = null
) {
    val isHealthy = result.label.contains("Healthy", ignoreCase = true)
    val isBBTV = result.label.contains("Bunchy Top", ignoreCase = true)

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
                            color = if (isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (isHealthy) Color(0xFF66BB6A) else Color(0xFFFF9800),
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

            // Row 1: AI Confidence + Severity
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
                    label = "Severity",
                    value = info.severity,
                    icon = Icons.Default.ErrorOutline,
                    accentColor = if (isHealthy) Color(0xFF66BB6A)
                    else when {
                        info.severity.contains("Critical", ignoreCase = true) -> Color(0xFFB71C1C)
                        info.severity.contains("Severe", ignoreCase = true) -> Color(0xFFEF5350)
                        info.severity.contains("Moderate", ignoreCase = true) -> Color(0xFFFF9800)
                        else -> Color(0xFF66BB6A)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: BBTV likelihood chip (only for BBTV scans with a verdict)
            if (isBBTV && bbtvVerdict != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val (likelihoodLabel, likelihoodColor) = when (bbtvVerdict) {
                    BBTVVerdict.HIGH -> "High Likelihood" to Color(0xFFEF5350)
                    BBTVVerdict.MODERATE -> "Possible BBTV" to Color(0xFFFF9800)
                    BBTVVerdict.LOW -> "Low Likelihood" to Color(0xFF4CAF50)
                }
                SubtleMetricChip(
                    label = "BBTV Likelihood",
                    value = likelihoodLabel,
                    icon = Icons.Default.BugReport,
                    accentColor = likelihoodColor,
                    modifier = Modifier.fillMaxWidth()
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

@Composable
fun ClassBreakdownCard(allConfidences: Map<String, Float>) {
    val sorted = allConfidences.entries.sortedByDescending { it.value }
    val topLabel = sorted.firstOrNull()?.key ?: ""

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
                Surface(shape = CircleShape, color = Color(0xFF42A5F5).copy(alpha = 0.15f)) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFF42A5F5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Class Probability Breakdown",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            sorted.forEach { (label, score) ->
                val isTop = label == topLabel
                val barColor = if (isTop) Color(0xFF2E7D32) else Color(0xFF9E9E9E)
                val pct = (score * 100).toInt()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isTop) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isTop) Color(0xFF1B5E20) else Color(0xFF616161),
                        modifier = Modifier.width(140.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE0E0E0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(score.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$pct%",
                        fontSize = 12.sp,
                        fontWeight = if (isTop) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isTop) Color(0xFF1B5E20) else Color(0xFF616161),
                        modifier = Modifier.width(34.dp)
                    )
                }
            }
        }
    }
}

fun formatDetailedTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun BBTVVerdictBanner(bbtvResult: BBTVQuestionnaireResult) {
    val (backgroundColor, borderColor, icon, title, message) = when (bbtvResult.verdict) {
        BBTVVerdict.HIGH -> Tuple5(
            Color(0xFFFFEBEE),
            Color(0xFFEF5350),
            Icons.Default.Warning,
            "High BBTV Likelihood",
            "Multiple key indicators are present. Isolate this plant immediately, control aphids in the surrounding area, and contact your local agricultural officer before uprooting."
        )
        BBTVVerdict.MODERATE -> Tuple5(
            Color(0xFFFFF3E0),
            Color(0xFFFF9800),
            Icons.Default.Info,
            "Possible BBTV — Monitor Closely",
            "Some indicators are present but not conclusive. Monitor this plant for 7–14 days. If symptoms worsen or spread to nearby plants, rescan and consult an agricultural technician."
        )
        BBTVVerdict.LOW -> Tuple5(
            Color(0xFFE8F5E9),
            Color(0xFF4CAF50),
            Icons.Default.CheckCircle,
            "Low BBTV Likelihood",
            "Symptoms are more consistent with nutrient deficiency, water stress, or normal slow growth. Check soil conditions, irrigation, and fertilization before taking any action."
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color(0xFF424242),
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Based on your answers  •  Verification score: ${bbtvResult.score}/8",
                fontSize = 11.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun SampleImagesCard(label: String) {
    val allSamples = when {
        label.contains("Healthy", ignoreCase = true) -> listOf(
            R.drawable.sample_healthy_1,
            R.drawable.sample_healthy_2,
            R.drawable.sample_healthy_3,
            R.drawable.sample_healthy_4,
            R.drawable.sample_healthy_5,
            R.drawable.sample_healthy_6
        )
        label.contains("Sigatoka", ignoreCase = true) -> listOf(
            R.drawable.sample_black_sigatoka_1,
            R.drawable.sample_black_sigatoka_2,
            R.drawable.sample_black_sigatoka_3,
            R.drawable.sample_black_sigatoka_4,
            R.drawable.sample_black_sigatoka_5,
            R.drawable.sample_black_sigatoka_6
        )
        label.contains("Bunchy Top", ignoreCase = true) -> listOf(
            R.drawable.sample_bbtv_1,
            R.drawable.sample_bbtv_2,
            R.drawable.sample_bbtv_3,
            R.drawable.sample_bbtv_4,
            R.drawable.sample_bbtv_5,
            R.drawable.sample_bbtv_6
        )
        label.contains("Fusarium", ignoreCase = true) || label.contains("TR4", ignoreCase = true) -> listOf(
            R.drawable.sample_fusarium_1,
            R.drawable.sample_fusarium_2,
            R.drawable.sample_fusarium_3,
            R.drawable.sample_fusarium_4,
            R.drawable.sample_fusarium_5,
            R.drawable.sample_fusarium_6
        )
        else -> emptyList()
    }

    if (allSamples.isEmpty()) return

    // Pick 3 random samples — re-randomized each time this screen is shown
    val displayed = remember(label) { allSamples.shuffled().take(3) }
    var viewerRes by remember { mutableStateOf<Int?>(null) }

    viewerRes?.let { res ->
        SampleImageViewerDialog(resId = res, onDismiss = { viewerRes = null })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFF8D6E63).copy(alpha = 0.15f)) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF8D6E63),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reference Samples",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = "Compare your image with known cases",
                        fontSize = 11.sp,
                        color = Color(0xFF757575)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 thumbnails in a horizontal row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayed.forEachIndexed { index, resId ->
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable { viewerRes = resId }
                    ) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = "Sample ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Tap-to-enlarge badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(5.dp)
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Tap any image to enlarge",
                fontSize = 10.sp,
                color = Color(0xFF9E9E9E),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun SampleImageViewerDialog(resId: Int, onDismiss: () -> Unit) {
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
            Image(
                painter = painterResource(id = resId),
                contentDescription = "Sample image full view",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
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
        }
    }
}
