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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsPage(onNavigateBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Name Field
        ContactTextField(
            label = "Your Name",
            value = name,
            onValueChange = { name = it },
            placeholder = "Your Name"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field
        ContactTextField(
            label = "Email Address",
            value = email,
            onValueChange = { email = it },
            placeholder = "Email Address"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subject Field
        ContactTextField(
            label = "Subject",
            value = subject,
            onValueChange = { subject = it },
            placeholder = "Subject"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Message Field
        ContactTextField(
            label = "Your Message",
            value = message,
            onValueChange = { message = it },
            placeholder = "Your Message",
            minLines = 5,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Send Message Button
        Button(
            onClick = { /* TODO: Send message */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD54F)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
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

        Spacer(modifier = Modifier.height(24.dp))
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
    maxLines: Int = 1
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                focusedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                unfocusedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
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
