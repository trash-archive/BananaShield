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
import androidx.core.content.ContextCompat
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

@Composable
fun ModernCameraScreen(
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var classification by remember { mutableStateOf<Classification?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val classifier = remember { BananaClassifier(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

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

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    newImageCapture
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
                    }
                )
            }
            previewBitmap != null -> {
                ImagePreviewMode(
                    bitmap = previewBitmap!!,
                    isAnalyzing = isAnalyzing,
                    onBack = {
                        previewBitmap = null
                        capturedBitmap = null
                    },
                    onAnalyze = {
                        isAnalyzing = true
                        classification = classifier.classify(previewBitmap!!)
                        capturedBitmap = previewBitmap
                        isAnalyzing = false
                        showResults = true
                    }
                )
            }
            else -> {
                CameraMode(
                    previewView = previewView,
                    flashEnabled = flashEnabled,
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
            ScanGuideDialog(onDismiss = { showGuide = false })
        }
    }
}

@Composable
fun CameraMode(
    previewView: PreviewView,
    flashEnabled: Boolean,
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

        // Disclaimer pill overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (navigationBarHeight + 120).dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1B5E20).copy(alpha = 0.85f),
                            Color(0xFF2E7D32).copy(alpha = 0.85f)
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
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = Color(0xFFA5D6A7),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Point camera at a banana leaf to scan",
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
                        color = Color(0xFFFFD54F),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(80.dp)
                    ) {
                        IconButton(
                            onClick = onCapture,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Capture",
                                tint = Color(0xFF1B5E20),
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
    onBack: () -> Unit,
    onAnalyze: () -> Unit
) {
    val density = LocalDensity.current
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density) / density.density

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

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

            Spacer(modifier = Modifier.weight(1f))

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
fun ScanGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Scanning Tips",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GuideItem(
                    icon = Icons.Default.WbSunny,
                    title = "Good Lighting",
                    description = "Take photos in bright, natural light for best results"
                )
                Spacer(modifier = Modifier.height(12.dp))
                GuideItem(
                    icon = Icons.Default.PhotoSizeSelectLarge,
                    title = "Fill the Frame",
                    description = "Position the leaf to fill most of the camera view"
                )
                Spacer(modifier = Modifier.height(12.dp))
                GuideItem(
                    icon = Icons.Default.CenterFocusStrong,
                    title = "Focus on Symptoms",
                    description = "Capture affected areas clearly and in focus"
                )
                Spacer(modifier = Modifier.height(12.dp))
                GuideItem(
                    icon = Icons.Default.Block,
                    title = "Avoid Blur",
                    description = "Hold steady and avoid moving while capturing"
                )
                Spacer(modifier = Modifier.height(12.dp))
                GuideItem(
                    icon = Icons.Default.Nature,
                    title = "Clean Background",
                    description = "Use a plain surface behind the leaf when possible"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it!", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
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
                .background(Color(0xFFF1F8E9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF689F38),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1B5E20)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF757575),
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