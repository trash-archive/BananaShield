package com.example.bananashield

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Context
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun ScanContent(
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        ModernCameraScreen(paddingValues, onNavigateBack)
    } else {
        ModernPermissionDeniedScreen(paddingValues, onNavigateBack) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
}

private const val PREFS_NAME = "scan_prefs"
private const val KEY_SKIP_GUIDE = "skip_scan_guide"

@Composable
fun ModernCameraScreen(
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val classifier = remember { BananaClassifier(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var classification by remember { mutableStateOf<Classification?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var plantLabel by remember { mutableStateOf("") }

    // Run classify off the main thread when isAnalyzing is set to true
    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing && previewBitmap != null) {
            classification = withContext(Dispatchers.Default) {
                classifier.classify(previewBitmap!!)
            }.copy(plantLabel = plantLabel)
            capturedBitmap = previewBitmap
            isAnalyzing = false
            showResults = true
        }
    }

    var flashEnabled by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var showGuide by remember { mutableStateOf(!prefs.getBoolean(KEY_SKIP_GUIDE, false)) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var liveValidation by remember { mutableStateOf<LeafValidation>(LeafValidation.Pending) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                previewBitmap = bitmap
                capturedBitmap = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BackHandler {
        when {
            showResults -> {
                showResults = false
                previewBitmap = null
                capturedBitmap = null
                classification = null
            }
            previewBitmap != null -> {
                previewBitmap = null
                capturedBitmap = null
            }
            else -> onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            classifier.close()
            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
        }
    }


    LaunchedEffect(flashEnabled) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val newImageCapture = ImageCapture.Builder()
                    .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                imageCapture = newImageCapture

                val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                    .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val bitmap = imageProxy.toBitmap()
                            val rotated = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
                            // runBlocking is safe here — cameraExecutor is a background thread
                            val result = runBlocking { classifier.validateLeaf(rotated) }
                            liveValidation = result
                            imageProxy.close()
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    newImageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(paddingValues)
    ) {
        when {
            showResults -> {
                ResultsScreen(
                    bitmap = capturedBitmap ?: previewBitmap,
                    classification = classification,
                    onScanAgain = {
                        showResults = false
                        capturedBitmap = null
                        previewBitmap = null
                        classification = null
                        plantLabel = ""
                    },
                    onAbortBBTV = {
                        // Back from BBTV questionnaire — return to image preview, don't save
                        showResults = false
                        capturedBitmap = null
                        classification = null
                        // previewBitmap intentionally kept so ImagePreviewMode is shown
                    }
                )
            }
            previewBitmap != null -> {
                ImagePreviewMode(
                    bitmap = previewBitmap!!,
                    isAnalyzing = isAnalyzing,
                    plantLabel = plantLabel,
                    onPlantLabelChange = { plantLabel = it },
                    onBack = {
                        previewBitmap = null
                        capturedBitmap = null
                    },
                    onAnalyze = {
                        isAnalyzing = true
                    }
                )
            }
            else -> {
                CameraMode(
                    previewView = previewView,
                    flashEnabled = flashEnabled,
                    liveValidation = liveValidation,
                    onFlashToggle = { flashEnabled = !flashEnabled },
                    onBack = onNavigateBack,
                    onShowGuide = { showGuide = true },
                    onCapture = {
                        imageCapture?.let { capture ->
                            capture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        val bitmap = imageProxy.toBitmap()
                                        val rotatedBitmap = rotateBitmap(
                                            bitmap,
                                            imageProxy.imageInfo.rotationDegrees.toFloat()
                                        )
                                        previewBitmap = rotatedBitmap
                                        imageProxy.close()
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                    }
                                }
                            )
                        }
                    },
                    onGallery = {
                        galleryLauncher.launch("image/*")
                    }
                )
            }
        }

        // Guide Dialog
        if (showGuide) {
            ScanGuideDialog(
                onDismiss = { showGuide = false },
                onDontShowAgainChanged = { skip ->
                    prefs.edit().putBoolean(KEY_SKIP_GUIDE, skip).apply()
                }
            )
        }
    }
}

@Composable
fun CameraMode(
    previewView: PreviewView,
    flashEnabled: Boolean,
    liveValidation: LeafValidation,
    onFlashToggle: () -> Unit,
    onBack: () -> Unit,
    onShowGuide: () -> Unit,
    onCapture: () -> Unit,
    onGallery: () -> Unit
) {
    val density = LocalDensity.current
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density) / density.density

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Gradient overlay at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Gradient overlay at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((240 + navigationBarHeight).dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Live validation overlay pill
        val (pillColor, pillText, pillIcon) = when (liveValidation) {
            is LeafValidation.Valid ->
                Triple(Color(0xFF2E7D32), "Banana leaf detected", Icons.Default.CheckCircle)
            is LeafValidation.RejectedByObject ->
                Triple(Color(0xFFB71C1C), "Not a banana leaf: ${liveValidation.detectedLabel}", Icons.Default.Warning)
            is LeafValidation.RejectedByConfidence ->
                Triple(Color(0xFFE65100), "No banana leaf detected", Icons.Default.Warning)
            is LeafValidation.RejectedByEntropy ->
                Triple(Color(0xFFE65100), "Looks similar but not a banana leaf", Icons.Default.Warning)
            is LeafValidation.Pending ->
                Triple(Color(0xFF424242), "Point camera at a banana leaf to scan", Icons.Default.Eco)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            pillColor.copy(alpha = 0.85f),
                            pillColor.copy(alpha = 0.75f)
                        )
                    ),
                    shape = RoundedCornerShape(50.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pillIcon,
                        contentDescription = null,
                        tint = Color(0xFFA5D6A7),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = pillText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = "Scan Banana Leaf",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = CircleShape,
                    color = if (flashEnabled) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.2f)
                ) {
                    IconButton(onClick = onFlashToggle) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (flashEnabled) Color(0xFF1B5E20) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = (navigationBarHeight + 24).dp,
                        start = 20.dp,
                        end = 20.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        IconButton(onClick = onGallery) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (liveValidation is LeafValidation.Valid) Color(0xFFFFD54F) else Color(0xFF616161),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(80.dp)
                    ) {
                        IconButton(
                            onClick = onCapture,
                            enabled = liveValidation is LeafValidation.Valid,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Capture",
                                tint = if (liveValidation is LeafValidation.Valid) Color(0xFF1B5E20) else Color(0xFF9E9E9E),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        IconButton(onClick = onShowGuide) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Guide",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImagePreviewMode(
    bitmap: Bitmap,
    isAnalyzing: Boolean,
    plantLabel: String,
    onPlantLabelChange: (String) -> Unit,
    onBack: () -> Unit,
    onAnalyze: () -> Unit
) {
    val density = LocalDensity.current
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density) / density.density
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density
    var showLabelInfo by remember { mutableStateOf(false) }

    if (showLabelInfo) {
        Dialog(onDismissRequest = { showLabelInfo = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1B3A2D), Color(0xFF0F2318))
                        )
                    )
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF2E7D32).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Why label your plant?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showLabelInfo = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "A plant label helps you identify which specific banana plant this scan belongs to — especially useful when you have many plants in your farm.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "Examples: \"Row 3, Plant 7\", \"Near the fence\", \"Sucker from Plant A\"",
                            fontSize = 13.sp,
                            color = Color(0xFF81C784),
                            lineHeight = 19.sp
                        )
                        Text(
                            text = "The label will appear in your scan history so you can quickly find and track the health of each plant over time.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Button(
                            onClick = { showLabelInfo = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Got it!", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Dark scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // Top gradient for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((statusBarHeight + 160).dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarHeight.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Top bar: back + title ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = "Preview",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Plant label field ──
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "PLANT LABEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA5D6A7),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1B5E20).copy(alpha = 0.82f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = Color(0xFF66BB6A).copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(20.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = plantLabel,
                            onValueChange = onPlantLabelChange,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp, top = 13.dp, bottom = 13.dp),
                            decorationBox = { inner ->
                                if (plantLabel.isEmpty()) {
                                    Text(
                                        text = "e.g. Row 3, Plant 7",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 15.sp
                                    )
                                }
                                inner()
                            }
                        )
                        if (plantLabel.isNotEmpty()) {
                            IconButton(
                                onClick = { onPlantLabelChange("") },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        // Info button
                        IconButton(
                            onClick = { showLabelInfo = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "What is this?",
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Analyze button ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = (navigationBarHeight + 24).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF66BB6A),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(80.dp)
                ) {
                    IconButton(
                        onClick = onAnalyze,
                        enabled = !isAnalyzing,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 4.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Analyze",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanGuideDialog(onDismiss: () -> Unit, onDontShowAgainChanged: (Boolean) -> Unit) {
    var viewerRes by remember { mutableStateOf<Int?>(null) }
    var dontShowAgain by remember { mutableStateOf(false) }

    viewerRes?.let { res ->
        LookAlikeImageViewerDialog(resId = res, onDismiss = { viewerRes = null })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B3A2D), Color(0xFF0F2318))
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Scanning Tips",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.08f))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    GuideItem(
                        icon = Icons.Default.WbSunny,
                        title = "Good Lighting",
                        description = "Take photos in bright, natural light for best results"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GuideItem(
                        icon = Icons.Default.PhotoSizeSelectLarge,
                        title = "Fill the Frame",
                        description = "Position the leaf to fill most of the camera view"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GuideItem(
                        icon = Icons.Default.CenterFocusStrong,
                        title = "Focus on Symptoms",
                        description = "Capture affected areas clearly and in focus"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GuideItem(
                        icon = Icons.Default.Block,
                        title = "Avoid Blur",
                        description = "Hold steady and avoid moving while capturing"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GuideItem(
                        icon = Icons.Default.Nature,
                        title = "Clean Background",
                        description = "Use a plain surface behind the leaf when possible"
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Section divider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                        Text(
                            text = "  NOT A BANANA LEAF  ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF9A9A),
                            letterSpacing = 1.sp
                        )
                        Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "These plants are commonly mistaken for banana leaves.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    lookAlikePlants.forEach { plant ->
                        LookAlikeItem(
                            plant = plant,
                            onImageClick = { viewerRes = plant.drawableRes }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Footer
                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                dontShowAgain = !dontShowAgain
                                onDontShowAgainChanged(dontShowAgain)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { checked ->
                                dontShowAgain = checked
                                onDontShowAgainChanged(checked)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF4CAF50),
                                uncheckedColor = Color.White.copy(alpha = 0.5f),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Don't show again",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Got it!", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun LookAlikeImageViewerDialog(resId: Int, onDismiss: () -> Unit) {
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
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = resId),
                contentDescription = null,
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

data class LookAlikePlant(
    val name: String,
    val hint: String,
    val drawableRes: Int
)

val lookAlikePlants = listOf(
    LookAlikePlant(
        name = "Heliconia",
        hint = "Narrower, waxy leaf with a prominent midrib. Often has red or orange flower bracts nearby.",
        drawableRes = R.drawable.lookalike_heliconia
    ),
    LookAlikePlant(
        name = "Bird of Paradise",
        hint = "Stiffer, paddle-shaped leaf with a long petiole. Splits along the midrib in wind.",
        drawableRes = R.drawable.lookalike_bird_of_paradise
    ),
    LookAlikePlant(
        name = "Canna Lily",
        hint = "Smaller, more oval leaf with visible parallel veins and a reddish or green stem.",
        drawableRes = R.drawable.lookalike_canna_lily
    ),
    LookAlikePlant(
        name = "Taro / Elephant Ear",
        hint = "Heart-shaped base where the stem attaches to the center of the leaf, not the edge.",
        drawableRes = R.drawable.lookalike_taro
    ),
    LookAlikePlant(
        name = "Traveller's Palm",
        hint = "Fan-shaped arrangement of leaves on a single plane. Leaves are more rigid and upright.",
        drawableRes = R.drawable.lookalike_travellers_palm
    ),
    LookAlikePlant(
        name = "Ginger Plant",
        hint = "Narrower leaf tapering to a sharp tip. Strong aromatic smell when the leaf is crushed.",
        drawableRes = R.drawable.lookalike_ginger
    )
)

@Composable
fun LookAlikeItem(plant: LookAlikePlant, onImageClick: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Box {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = plant.drawableRes),
                contentDescription = plant.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onImageClick() },
                contentScale = ContentScale.Crop
            )
            // Zoom hint badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFEF9A9A), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = plant.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = plant.hint,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun GuideItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF2E7D32).copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF81C784),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ModernPermissionDeniedScreen(
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit,
    onRequestPermission: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B5E20),
                        Color(0xFF2E7D32)
                    )
                )
            )
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Camera Access Needed",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "We need camera permission to scan and identify banana leaf diseases accurately",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD54F)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Grant Camera Permission",
                    color = Color(0xFF1B5E20),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your privacy is protected. Images are processed locally.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ✅ FIXED: Proper YUV to Bitmap conversion for CameraX
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = android.graphics.YuvImage(
        nv21,
        android.graphics.ImageFormat.NV21,
        width,
        height,
        null
    )

    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun centerCropBitmap(bitmap: Bitmap, targetSize: Int = 224): Bitmap {
    val size = minOf(bitmap.width, bitmap.height)
    val x = (bitmap.width - size) / 2
    val y = (bitmap.height - size) / 2
    val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
    return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
}