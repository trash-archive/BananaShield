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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyPage(onNavigateBack: () -> Unit) {
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
                            text = "Privacy Policy",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Your privacy matters to us",
                            fontSize = 13.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ Last Updated Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Last Updated",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        text = "October 4, 2025",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Policy Sections
        ModernPrivacySection(
            icon = Icons.Default.Info,
            iconColor = Color(0xFF2196F3),
            iconBackground = Color(0xFFE3F2FD),
            number = "1",
            title = "Information We Collect",
            content = "We collect information you provide directly, including name, email, phone number, and farm details for account creation. This information helps us provide personalized services and improve your experience with BananaShield."
        )

        Spacer(modifier = Modifier.height(12.dp))

        ModernPrivacySection(
            icon = Icons.Default.Image,
            iconColor = Color(0xFF4CAF50),
            iconBackground = Color(0xFFE8F5E9),
            number = "2",
            title = "Image Data Usage",
            content = "Plant images are processed for disease detection using our AI model. Images are stored securely in encrypted storage and used only for providing scan results and improving our detection accuracy."
        )

        Spacer(modifier = Modifier.height(12.dp))

        ModernPrivacySection(
            icon = Icons.Default.Shield,
            iconColor = Color(0xFFFF9800),
            iconBackground = Color(0xFFFFF3E0),
            number = "3",
            title = "Data Protection",
            content = "We use industry-standard encryption to protect your data both in transit and at rest. Your personal information is never sold to third parties. We implement strict security measures to prevent unauthorized access."
        )

        Spacer(modifier = Modifier.height(12.dp))

        ModernPrivacySection(
            icon = Icons.Default.LocationOn,
            iconColor = Color(0xFFEF5350),
            iconBackground = Color(0xFFFFEBEE),
            number = "4",
            title = "Location Information",
            content = "Farm location data is used to provide localized disease alerts and agricultural recommendations specific to your region. This helps us deliver relevant information about disease patterns in your area."
        )

        Spacer(modifier = Modifier.height(12.dp))

        ModernPrivacySection(
            icon = Icons.Default.Share,
            iconColor = Color(0xFF9C27B0),
            iconBackground = Color(0xFFF3E5F5),
            number = "5",
            title = "Data Sharing",
            content = "Anonymized and aggregated data may be shared with agricultural researchers to improve disease detection methods and contribute to plant health research. Individual user data is never shared without explicit consent."
        )

        Spacer(modifier = Modifier.height(12.dp))

        ModernPrivacySection(
            icon = Icons.Default.VerifiedUser,
            iconColor = Color(0xFF00BCD4),
            iconBackground = Color(0xFFE0F7FA),
            number = "6",
            title = "Your Rights",
            content = "You have full control over your data. You can access, update, export, or permanently delete your data at any time through your account settings. We respect your privacy choices and will honor all deletion requests promptly."
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ Contact Section
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
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Questions about Privacy?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We're here to help. Contact our privacy team:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "privacy@bananashield.com",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Footer
        Text(
            text = "By using BananaShield, you agree to our Privacy Policy",
            fontSize = 12.sp,
            color = Color(0xFF9E9E9E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
fun ModernPrivacySection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBackground: Color,
    number: String,
    title: String,
    content: String
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
                .padding(16.dp)
        ) {
            // Icon
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
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = iconColor
                    ) {
                        Text(
                            text = number,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = Color(0xFF424242),
                    lineHeight = 22.sp
                )
            }
        }
    }
}
