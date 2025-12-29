package com.example.bananashield

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordPage(onNavigateBack: () -> Unit) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isChanging by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // ✅ Get status bar height
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density

    // ✅ ADD BACK HANDLER
    BackHandler(enabled = true) {
        onNavigateBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .verticalScroll(rememberScrollState())
    ) {
        // ✅ Modern Header
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
                            text = "Change Password",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Update your account security",
                            fontSize = 13.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ Hero Card with Lock Icon
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Secure Your Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Create a strong password to protect your account and keep your data safe.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Current Password
        ModernPasswordField(
            label = "Current Password",
            value = currentPassword,
            onValueChange = {
                currentPassword = it
                errorMessage = null
            },
            placeholder = "Enter your current password",
            icon = Icons.Default.Lock,
            iconColor = Color(0xFFEF5350),
            iconBackground = Color(0xFFFFEBEE),
            isVisible = currentPasswordVisible,
            onVisibilityChange = { currentPasswordVisible = !currentPasswordVisible },
            enabled = !isChanging
        )

        Spacer(modifier = Modifier.height(12.dp))

        // New Password
        ModernPasswordField(
            label = "New Password",
            value = newPassword,
            onValueChange = {
                newPassword = it
                errorMessage = null
            },
            placeholder = "Enter your new password",
            icon = Icons.Default.VpnKey,
            iconColor = Color(0xFF2196F3),
            iconBackground = Color(0xFFE3F2FD),
            isVisible = newPasswordVisible,
            onVisibilityChange = { newPasswordVisible = !newPasswordVisible },
            enabled = !isChanging
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm Password
        ModernPasswordField(
            label = "Confirm New Password",
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
            },
            placeholder = "Confirm your new password",
            icon = Icons.Default.CheckCircle,
            iconColor = Color(0xFF4CAF50),
            iconBackground = Color(0xFFE8F5E9),
            isVisible = confirmPasswordVisible,
            onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible },
            enabled = !isChanging
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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

        // Change Password Button
        Button(
            onClick = {
                errorMessage = null

                when {
                    currentPassword.isBlank() -> errorMessage = "Please enter current password"
                    newPassword.isBlank() -> errorMessage = "Please enter new password"
                    newPassword.length < 6 -> errorMessage = "Password must be at least 6 characters"
                    newPassword != confirmPassword -> errorMessage = "Passwords do not match"
                    newPassword == currentPassword -> errorMessage = "New password must be different"
                    else -> {
                        isChanging = true
                        val email = currentUser?.email ?: ""
                        val credential = EmailAuthProvider.getCredential(email, currentPassword)

                        currentUser?.reauthenticate(credential)
                            ?.addOnSuccessListener {
                                currentUser.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        isChanging = false
                                        showSuccessDialog = true
                                    }
                                    .addOnFailureListener { exception ->
                                        isChanging = false
                                        errorMessage = "Failed to update password: ${exception.message}"
                                    }
                            }
                            ?.addOnFailureListener { exception ->
                                isChanging = false
                                errorMessage = "Current password is incorrect"
                            }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D32),
                disabledContainerColor = Color(0xFF66BB6A),
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = !isChanging
        ) {
            if (isChanging) {
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
                        text = "Updating...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Change Password",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Password Requirements Card
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Password Requirements",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                PasswordRequirementItem("At least 6 characters long")
                PasswordRequirementItem("Mix of uppercase and lowercase letters")
                PasswordRequirementItem("Include numbers and special characters")
                PasswordRequirementItem("Different from current password")
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }

    // Success Dialog
    if (showSuccessDialog) {
        ModernPasswordSuccessDialog(
            onDismiss = {
                showSuccessDialog = false
                onNavigateBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBackground: Color,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF757575)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = Color(0xFFBDBDBD)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onVisibilityChange) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isVisible) "Hide password" else "Show password",
                            tint = Color(0xFF757575)
                        )
                    }
                },
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF1B5E20),
                    unfocusedTextColor = Color(0xFF424242),
                    disabledTextColor = Color(0xFF424242),
                    focusedBorderColor = Color(0xFF2E7D32),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    disabledBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFF5F7FA),
                    unfocusedContainerColor = Color(0xFFF5F7FA),
                    disabledContainerColor = Color(0xFFF5F7FA),
                    cursorColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled
            )
        }
    }
}

@Composable
fun PasswordRequirementItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .offset(y = 6.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF424242),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun ModernPasswordSuccessDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { }) {
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
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Password Changed!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your password has been successfully updated. Your account is now more secure!",
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Done",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
