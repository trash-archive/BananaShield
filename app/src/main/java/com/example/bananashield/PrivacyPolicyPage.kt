package com.example.bananashield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyPage(onNavigateBack: () -> Unit) {
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
                    text = "Privacy Policy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Last Updated
            Text(
                text = "Last updated: October 4, 2025",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Policy Sections
            PrivacyPolicySection(
                number = "1",
                title = "Information We Collect",
                content = "We collect information you provide directly, including name, email, phone number, and farm details for account creation."
            )

            PrivacyPolicySection(
                number = "2",
                title = "Image Data Usage",
                content = "Plant images are processed for disease detection. Images are stored securely and used only for providing scan results."
            )

            PrivacyPolicySection(
                number = "3",
                title = "Data Protection",
                content = "We use industry-standard encryption to protect your data. Your information is never sold to third parties."
            )

            PrivacyPolicySection(
                number = "4",
                title = "Location Information",
                content = "Farm location is used to provide localized disease alerts and recommendations specific to your region."
            )

            PrivacyPolicySection(
                number = "5",
                title = "Data Sharing",
                content = "Anonymized data may be shared with agricultural researchers to improve disease detection and prevention methods."
            )

            PrivacyPolicySection(
                number = "6",
                title = "Your Rights",
                content = "You can access, update, or delete your data at any time through your account settings."
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Contact Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Questions about privacy?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Contact us at:",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "privacy@bananashield.com",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }


@Composable
fun PrivacyPolicySection(
    number: String,
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = "$number. $title",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 20.sp
        )
    }
}
