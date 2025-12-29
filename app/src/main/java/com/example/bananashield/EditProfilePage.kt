package com.example.bananashield

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfilePage(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var farmSize by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var permissionType by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ✅ Get status bar and navigation bar heights
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density) / density.density

    val userInitial = currentUser?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    // ✅ Handle system back button press
    BackHandler(enabled = true) {
        onNavigateBack()
    }

    // ... [rest of your functions remain the same - createImageUri, launchers, etc.]

    fun createImageUri(): Uri {
        val directory = File(context.cacheDir, "images")
        directory.mkdirs()
        val file = File.createTempFile(
            "camera_photo_",
            ".jpg",
            directory
        )
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(
            context,
            authority,
            file
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                profileImageUri = uri
                isUploadingImage = true
                currentUser?.uid?.let { userId ->
                    StorageHelper.uploadProfileImage(
                        userId = userId,
                        imageUri = uri,
                        onSuccess = { downloadUrl ->
                            profileImageUrl = downloadUrl
                            isUploadingImage = false
                        },
                        onFailure = { exception ->
                            errorMessage = "Failed to upload image: ${exception.message}"
                            isUploadingImage = false
                        }
                    )
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            isUploadingImage = true
            currentUser?.uid?.let { userId ->
                StorageHelper.uploadProfileImage(
                    userId = userId,
                    imageUri = it,
                    onSuccess = { downloadUrl ->
                        profileImageUrl = downloadUrl
                        isUploadingImage = false
                    },
                    onFailure = { exception ->
                        errorMessage = "Failed to upload image: ${exception.message}"
                        isUploadingImage = false
                    }
                )
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createImageUri()
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            errorMessage = "Camera permission denied"
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        } else {
            errorMessage = "Storage permission denied"
        }
    }

    fun checkAndRequestCameraPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                val uri = createImageUri()
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            }
            else -> {
                permissionType = "camera"
                showPermissionDialog = true
            }
        }
    }

    fun checkAndRequestGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, permission) -> {
                galleryLauncher.launch("image/*")
            }
            else -> {
                permissionType = "gallery"
                showPermissionDialog = true
            }
        }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            FirestoreHelper.getUserData(
                userId = userId,
                onSuccess = { data ->
                    firstName = data["firstName"] as? String ?: ""
                    lastName = data["lastName"] as? String ?: ""
                    phone = data["phone"] as? String ?: ""
                    location = data["location"] as? String ?: ""
                    farmSize = data["farmSize"] as? String ?: ""
                    profileImageUrl = data["profileImageUrl"] as? String
                    isLoading = false
                },
                onFailure = {
                    val nameParts = currentUser.displayName?.split(" ") ?: listOf("", "")
                    firstName = nameParts.getOrNull(0) ?: ""
                    lastName = nameParts.getOrNull(1) ?: ""
                    isLoading = false
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .verticalScroll(rememberScrollState())
        ) {
            // Modern Header with status bar padding
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
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF2E7D32)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "Edit Profile",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = "Update your information",
                                fontSize = 13.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Picture Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUri != null || !profileImageUrl.isNullOrEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = profileImageUri ?: profileImageUrl
                                    ),
                                    contentDescription = "Profile Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = userInitial,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            if (isUploadingImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(40.dp),
                                        strokeWidth = 4.dp
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .offset(x = 4.dp, y = 4.dp)
                                .clickable {
                                    if (!isUploadingImage) showImageSourceDialog = true
                                },
                            shape = CircleShape,
                            color = Color(0xFF2E7D32),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Change photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Change Profile Picture",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.clickable {
                            if (!isUploadingImage) showImageSourceDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else {
                // Personal Information
                Text(
                    text = "Personal Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernTextField(
                    label = "First Name",
                    value = firstName,
                    onValueChange = { firstName = it },
                    icon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernTextField(
                    label = "Last Name",
                    value = lastName,
                    onValueChange = { lastName = it },
                    icon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernTextField(
                    label = "Email Address",
                    value = email,
                    onValueChange = { },
                    icon = Icons.Default.Email,
                    enabled = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernTextField(
                    label = "Phone Number",
                    value = phone,
                    onValueChange = { phone = it },
                    icon = Icons.Default.Phone
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Farm Details
                Text(
                    text = "Farm Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernTextField(
                    label = "Location",
                    value = location,
                    onValueChange = { location = it },
                    icon = Icons.Default.LocationOn
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernTextField(
                    label = "Farm Size (Hectares)",
                    value = farmSize,
                    onValueChange = { farmSize = it },
                    icon = Icons.Default.Landscape
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFEF5350),
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Save Button
                Button(
                    onClick = {
                        errorMessage = null

                        if (firstName.isBlank() || lastName.isBlank()) {
                            errorMessage = "First name and last name are required"
                            return@Button
                        }

                        isSaving = true
                        currentUser?.uid?.let { userId ->
                            val profileUpdates = userProfileChangeRequest {
                                displayName = "$firstName $lastName"
                                profileImageUrl?.let { photoUri = Uri.parse(it) }
                            }

                            currentUser.updateProfile(profileUpdates)
                                .addOnCompleteListener { authTask ->
                                    if (authTask.isSuccessful) {
                                        FirestoreHelper.updateUserData(
                                            userId = userId,
                                            firstName = firstName,
                                            lastName = lastName,
                                            phone = phone,
                                            location = location,
                                            farmSize = farmSize,
                                            profileImageUrl = profileImageUrl,
                                            onSuccess = {
                                                isSaving = false
                                                showSuccessDialog = true
                                            },
                                            onFailure = { exception ->
                                                isSaving = false
                                                errorMessage = "Failed to save: ${exception.message}"
                                            }
                                        )
                                    } else {
                                        isSaving = false
                                        errorMessage = "Failed to update profile: ${authTask.exception?.message}"
                                    }
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaving) Color(0xFF66BB6A) else Color(0xFF2E7D32),
                        disabledContainerColor = Color(0xFF66BB6A)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSaving && !isUploadingImage
                ) {
                    if (isSaving) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Saving...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // ✅ Add navigation bar padding at the bottom
                Spacer(modifier = Modifier.height((navigationBarHeight + 16).dp))
            }
        }

        // ... [Dialogs remain the same]
        if (showImageSourceDialog) {
            ModernImageSourceDialog(
                onTakePhoto = {
                    showImageSourceDialog = false
                    checkAndRequestCameraPermission()
                },
                onChooseGallery = {
                    showImageSourceDialog = false
                    checkAndRequestGalleryPermission()
                },
                onDismiss = { showImageSourceDialog = false }
            )
        }

        if (showSuccessDialog) {
            ModernProfileSavedDialog(
                onDismiss = {
                    showSuccessDialog = false
                    onNavigateBack()
                }
            )
        }

        if (showPermissionDialog) {
            PermissionRequestDialog(
                permissionType = permissionType,
                onConfirm = {
                    showPermissionDialog = false
                    if (permissionType == "camera") {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        galleryPermissionLauncher.launch(permission)
                    }
                },
                onDismiss = {
                    showPermissionDialog = false
                    errorMessage = "Permission is required to ${if (permissionType == "camera") "take photos" else "select photos"}"
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (enabled) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) Color(0xFF2E7D32) else Color(0xFFBDBDBD),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) Color(0xFF757575) else Color(0xFFBDBDBD)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF1B5E20),
                    unfocusedTextColor = Color(0xFF1B5E20),
                    disabledTextColor = Color(0xFF9E9E9E),
                    focusedBorderColor = Color(0xFF2E7D32),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    disabledBorderColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color(0xFFF5F7FA),
                    unfocusedContainerColor = Color(0xFFF5F7FA),
                    disabledContainerColor = Color(0xFFFAFAFA),
                    cursorColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun ModernImageSourceDialog(
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Choose Photo Source",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Take Photo Button
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Take Photo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Choose from Gallery Button
                OutlinedButton(
                    onClick = onChooseGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2E7D32)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = Color(0xFF2E7D32)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Choose from Gallery",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernProfileSavedDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFE8F5E9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Profile Updated!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your changes have been successfully saved",
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequestDialog(
    permissionType: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    imageVector = if (permissionType == "camera") Icons.Default.Camera else Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = if (permissionType == "camera") "Camera Access" else "Photo Access",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1B5E20)
            )
        },
        text = {
            Text(
                text = if (permissionType == "camera") {
                    "BananaShield needs camera access to take profile pictures"
                } else {
                    "BananaShield needs photo access to update your profile picture"
                },
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Allow", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF757575))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
