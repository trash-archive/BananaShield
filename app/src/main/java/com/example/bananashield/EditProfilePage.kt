package com.example.bananashield

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    val userInitial = currentUser?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    // Function to create image URI for camera
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

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                profileImageUri = uri
                // Upload image to Firebase Storage
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

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            // Upload image to Firebase Storage
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

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            val uri = createImageUri()
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            errorMessage = "Camera permission denied"
        }
    }

    // Gallery permission launcher
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, open gallery
            galleryLauncher.launch("image/*")
        } else {
            errorMessage = "Storage permission denied"
        }
    }

    // Function to check and request camera permission
    fun checkAndRequestCameraPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                // Permission already granted, launch camera
                val uri = createImageUri()
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            }
            else -> {
                // Request permission
                permissionType = "camera"
                showPermissionDialog = true
            }
        }
    }

    // Function to check and request gallery permission
    fun checkAndRequestGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, permission) -> {
                // Permission already granted
                galleryLauncher.launch("image/*")
            }
            else -> {
                // Request permission
                permissionType = "gallery"
                showPermissionDialog = true
            }
        }
    }

    // Load user data
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            FirestoreHelper.getUserData(
                userId = userId,
                onSuccess = { data ->
                    firstName = data["firstName"] as? String ?: ""
                    lastName = data["lastName"] as? String ?: ""
                    phone = data["phone"] as? String ?: ""
                    location = data["location"] as? String ?: "Cebu City, Philippines"
                    farmSize = data["farmSize"] as? String ?: "2.5"
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
                .background(Color(0xFF2E7D32))
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Profile Avatar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD54F)),
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
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }

                        // Upload indicator
                        if (isUploadingImage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B5E20))
                            .clickable {
                                if (!isUploadingImage) {
                                    showImageSourceDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Photo",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Change Photo",
                    fontSize = 14.sp,
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.clickable {
                        if (!isUploadingImage) {
                            showImageSourceDialog = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Personal Information Section
            Text(
                text = "Personal Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                // First Name
                EditProfileTextField(
                    label = "First Name",
                    value = firstName,
                    onValueChange = { firstName = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Last Name
                EditProfileTextField(
                    label = "Last Name",
                    value = lastName,
                    onValueChange = { lastName = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email (Read-only)
                EditProfileTextField(
                    label = "Email Address",
                    value = email,
                    onValueChange = { },
                    enabled = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Number
                EditProfileTextField(
                    label = "Phone Number",
                    value = phone,
                    onValueChange = { phone = it }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Farm Details Section
                Text(
                    text = "Farm Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Location
                EditProfileTextField(
                    label = "Location",
                    value = location,
                    onValueChange = { location = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Farm Size
                EditProfileTextField(
                    label = "Farm Size (Hectares)",
                    value = farmSize,
                    onValueChange = { farmSize = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF5252).copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
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
                            // Update display name in Firebase Auth
                            val profileUpdates = userProfileChangeRequest {
                                displayName = "$firstName $lastName"
                                profileImageUrl?.let { photoUri = Uri.parse(it) }
                            }

                            currentUser.updateProfile(profileUpdates)
                                .addOnCompleteListener { authTask ->
                                    if (authTask.isSuccessful) {
                                        // Update Firestore
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
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isSaving && !isUploadingImage
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF1B5E20),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Save",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }

                // Extra spacing at bottom
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // Image Source Selection Dialog
        if (showImageSourceDialog) {
            ImageSourceDialog(
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

        // Success Dialog
        if (showSuccessDialog) {
            ProfileSavedDialog(
                onDismiss = {
                    showSuccessDialog = false
                    onNavigateBack()
                }
            )
        }

        // Permission Dialog
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

@Composable
fun ImageSourceDialog(
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
                containerColor = Color(0xFF66BB6A)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose Photo Source",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Take Photo Button
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Camera",
                        tint = Color(0xFF1B5E20),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Take Photo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Choose from Gallery Button
                OutlinedButton(
                    onClick = onChooseGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Choose from Gallery",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel Button
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
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
            Icon(
                imageVector = if (permissionType == "camera") Icons.Default.Camera else Icons.Default.PhotoLibrary,
                contentDescription = "Permission",
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = if (permissionType == "camera") "Camera Access Required" else "Photo Access Required",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = if (permissionType == "camera") {
                    "BananaShield needs access to your camera to take profile pictures."
                } else {
                    "BananaShield needs access to your photos to update your profile picture."
                },
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32)
                )
            ) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF2E7D32))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                disabledBorderColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                focusedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                unfocusedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun ProfileSavedDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF66BB6A)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD54F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color(0xFF1B5E20),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Profile Saved!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your changes have been\nsuccessfully saved",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }
    }
}
