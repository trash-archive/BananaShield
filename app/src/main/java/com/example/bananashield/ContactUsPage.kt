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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsPage(onNavigateBack: () -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Auto-fill user data
    var name by remember {
        mutableStateOf(currentUser?.displayName ?: "Anonymous User")
    }
    val email = currentUser?.email ?: "No email"

    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // Form validation states
    var subjectError by remember { mutableStateOf(false) }
    var messageError by remember { mutableStateOf(false) }

    // Loading and success states
    var isSending by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // ✅ Get status bar height
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density

    // ✅ ADD BACK HANDLER
    BackHandler(enabled = true) {
        onNavigateBack()
    }

    // Success Dialog
    if (showSuccessDialog) {
        ContactSuccessDialog(
            onDismiss = {
                showSuccessDialog = false
                onNavigateBack()
            }
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        ContactErrorDialog(
            errorMessage = errorMessage,
            onDismiss = { showErrorDialog = false }
        )
    }

    // ✅ REMOVED LOADING DIALOG

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .verticalScroll(rememberScrollState())
    ) {
        // ✅ Modern Header with status bar padding
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
                            text = "Contact Us",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "We're here to help you",
                            fontSize = 13.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ Hero Card
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
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Contact",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Get in Touch",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Have questions? We'd love to hear from you. Send us a message and we'll respond as soon as possible.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form Section
        Text(
            text = "Send us a Message",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Name Field
        ModernContactTextField(
            label = "Your Name",
            value = name,
            onValueChange = { name = it },
            placeholder = "Enter your name",
            icon = Icons.Default.Person,
            isError = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email Field (Read-only)
        ModernContactTextField(
            label = "Email Address",
            value = email,
            onValueChange = { },
            placeholder = "Email Address",
            icon = Icons.Default.Email,
            readOnly = true,
            isError = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Subject Field
        ModernContactTextField(
            label = "Subject",
            value = subject,
            onValueChange = {
                subject = it
                subjectError = false
            },
            placeholder = "What's this about?",
            icon = Icons.Default.Subject,
            isError = subjectError
        )

        if (subjectError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Subject is required",
                    color = Color(0xFFEF5350),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Message Field
        ModernContactTextField(
            label = "Your Message",
            value = message,
            onValueChange = {
                message = it
                messageError = false
            },
            placeholder = "Tell us more details...",
            icon = Icons.Default.Message,
            minLines = 5,
            maxLines = 8,
            isError = messageError
        )

        if (messageError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Message is required",
                    color = Color(0xFFEF5350),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ UPDATED BUTTON WITH LOADING STATE
        Button(
            onClick = {
                // Form validation
                var hasError = false

                if (subject.isBlank()) {
                    subjectError = true
                    hasError = true
                }

                if (message.isBlank()) {
                    messageError = true
                    hasError = true
                }

                if (!hasError) {
                    isSending = true

                    ContactHelper.sendContactMessage(
                        userName = name,
                        subject = subject,
                        message = message,
                        onSuccess = {
                            isSending = false
                            showSuccessDialog = true

                            // Clear form
                            subject = ""
                            message = ""
                        },
                        onFailure = { exception ->
                            isSending = false
                            errorMessage = exception.message ?: "Unknown error occurred"
                            showErrorDialog = true
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSending) Color(0xFF66BB6A) else Color(0xFF2E7D32),
                disabledContainerColor = Color(0xFF66BB6A)
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = !isSending
        ) {
            if (isSending) {
                // ✅ LOADING STATE
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
                        text = "Sending...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                // ✅ NORMAL STATE
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Message",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Other Contact Methods Section
        Text(
            text = "Other Ways to Reach Us",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email Contact
        ModernContactMethodCard(
            icon = Icons.Default.Email,
            iconColor = Color(0xFF2196F3),
            iconBackground = Color(0xFFE3F2FD),
            title = "Email Support",
            text = "support@bananashield.com"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Phone Contact
        ModernContactMethodCard(
            icon = Icons.Default.Phone,
            iconColor = Color(0xFF4CAF50),
            iconBackground = Color(0xFFE8F5E9),
            title = "Call Us",
            text = "+63 912 345 6789"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Location Contact
        ModernContactMethodCard(
            icon = Icons.Default.LocationOn,
            iconColor = Color(0xFFEF5350),
            iconBackground = Color(0xFFFFEBEE),
            title = "Visit Us",
            text = "Cebu City, Philippines"
        )

        Spacer(modifier = Modifier.height(64.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernContactTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    minLines: Int = 1,
    maxLines: Int = 1,
    readOnly: Boolean = false,
    isError: Boolean = false
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (readOnly) Color(0xFFE0E0E0)
                            else if (isError) Color(0xFFFFEBEE)
                            else Color(0xFFE8F5E9)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (readOnly) Icons.Default.Lock else icon,
                        contentDescription = null,
                        tint = if (readOnly) Color(0xFF9E9E9E)
                        else if (isError) Color(0xFFEF5350)
                        else Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isError) Color(0xFFEF5350) else Color(0xFF757575)
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
                readOnly = readOnly,
                isError = isError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF1B5E20),
                    unfocusedTextColor = Color(0xFF424242),
                    disabledTextColor = Color(0xFF9E9E9E),
                    focusedBorderColor = if (isError) Color(0xFFEF5350) else Color(0xFF2E7D32),
                    unfocusedBorderColor = if (isError) Color(0xFFEF5350) else Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFF5F7FA),
                    unfocusedContainerColor = Color(0xFFF5F7FA),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    cursorColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                minLines = minLines,
                maxLines = maxLines
            )
        }
    }
}

@Composable
fun ModernContactMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBackground: Color,
    title: String,
    text: String
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    fontSize = 15.sp,
                    color = Color(0xFF424242)
                )
            }
        }
    }
}

@Composable
fun ContactSuccessDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
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
                    text = "Message Sent!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Thank you for contacting us. We'll get back to you as soon as possible!",
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

@Composable
fun ContactErrorDialog(errorMessage: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Failed to Send",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage,
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Try Again",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
