package com.example.bananashield

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

    // Success Dialog
    if (showSuccessDialog) {
        Dialog(onDismissRequest = {
            showSuccessDialog = false
            onNavigateBack()
        }) {
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
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Message Sent!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thank you for contacting us. We'll get back to you soon!",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK", color = Color.White)
                    }
                }
            }
        }
    }

    // Error Dialog
    if (showErrorDialog) {
        Dialog(onDismissRequest = { showErrorDialog = false }) {
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
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Failed to Send",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showErrorDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5252)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Try Again", color = Color.White)
                    }
                }
            }
        }
    }

    // Loading Dialog
    if (isSending) {
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
                        text = "Sending Message...",
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
                text = "Contact Us",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Icon and Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD54F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Contact",
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We're here to help! Get in touch with us",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Name Field (Editable)
        ContactTextField(
            label = "Your Name",
            value = name,
            onValueChange = { name = it },
            placeholder = "Your Name",
            readOnly = false,
            isError = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field (Read-only - auto-filled)
        ContactTextField(
            label = "Email Address",
            value = email,
            onValueChange = { },
            placeholder = "Email Address",
            readOnly = true, // This makes it non-editable
            isError = false,
            leadingIcon = Icons.Default.Lock // Show lock icon to indicate read-only
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subject Field
        ContactTextField(
            label = "Subject",
            value = subject,
            onValueChange = {
                subject = it
                subjectError = false
            },
            placeholder = "What's this about?",
            readOnly = false,
            isError = subjectError
        )

        if (subjectError) {
            Text(
                text = "Subject is required",
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Message Field
        ContactTextField(
            label = "Your Message",
            value = message,
            onValueChange = {
                message = it
                messageError = false
            },
            placeholder = "Tell us more details...",
            minLines = 5,
            maxLines = 8,
            readOnly = false,
            isError = messageError
        )

        if (messageError) {
            Text(
                text = "Message is required",
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Send Message Button
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
                .padding(horizontal = 24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD54F)
            ),
            shape = RoundedCornerShape(28.dp),
            enabled = !isSending
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                tint = Color(0xFF1B5E20)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Send Message",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Other Ways to Reach Us
        Text(
            text = "Other Ways to Reach Us",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email Contact
        ContactMethodCard(
            icon = Icons.Default.Email,
            iconBackground = Color(0xFFFF5252),
            text = "support@bananashield.com"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Phone Contact
        ContactMethodCard(
            icon = Icons.Default.Phone,
            iconBackground = Color(0xFF4CAF50),
            text = "+63 912 345 6789"
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    maxLines: Int = 1,
    readOnly: Boolean = false,
    isError: Boolean = false,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null,
            readOnly = readOnly,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White.copy(alpha = 0.7f),
                focusedBorderColor = if (isError) Color(0xFFFF5252) else Color(0xFF4CAF50),
                unfocusedBorderColor = if (isError) Color(0xFFFF5252) else Color(0xFF4CAF50).copy(alpha = 0.5f),
                focusedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                unfocusedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            maxLines = maxLines
        )
    }
}

@Composable
fun ContactMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBackground: Color,
    text: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}
