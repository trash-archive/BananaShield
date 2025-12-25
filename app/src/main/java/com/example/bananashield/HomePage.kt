package com.example.bananashield

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun HomePage() {
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    val userName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "User"
    val userInitial = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    var selectedTab by remember { mutableStateOf(0) }
    var showProfile by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showFAQ by remember { mutableStateOf(false) }
    var showContactUs by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    when {
        showProfile -> {
            ProfilePage(
                paddingValues = PaddingValues(0.dp),
                onNavigateBack = { showProfile = false }
            )
        }
        showChangePassword -> {
            ChangePasswordPage(onNavigateBack = { showChangePassword = false })
        }
        showFAQ -> {
            FAQPage(onNavigateBack = { showFAQ = false })
        }
        showContactUs -> {
            ContactUsPage(onNavigateBack = { showContactUs = false })
        }
        showPrivacyPolicy -> {
            PrivacyPolicyPage(onNavigateBack = { showPrivacyPolicy = false })
        }
        else -> {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            ) { paddingValues ->
                when (selectedTab) {
                    0 -> HomeContent(
                        paddingValues = paddingValues,
                        userName = userName,
                        userInitial = userInitial,
                        onProfileClick = { showProfile = true }
                    )
                    1 -> NotificationContent(paddingValues)
                    2 -> ScanContent(paddingValues)
                    3 -> HistoryContent(paddingValues)
                    4 -> SettingsContent(
                        paddingValues = paddingValues,
                        onNavigateToChangePassword = { showChangePassword = true },
                        onNavigateToFAQ = { showFAQ = true },
                        onNavigateToContactUs = { showContactUs = true },
                        onNavigateToPrivacyPolicy = { showPrivacyPolicy = true }
                    )
                }
            }
        }
    }
}


@Composable
fun HomeContent(
    paddingValues: PaddingValues,
    userName: String,
    userInitial: String,
    onProfileClick: () -> Unit
) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    // Load user data for profile image
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            FirestoreHelper.getUserData(
                userId = userId,
                onSuccess = { data ->
                    userData = data
                    isLoadingProfile = false
                },
                onFailure = {
                    isLoadingProfile = false
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header with user greeting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar - Now clickable and shows profile image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD54F))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                val profileImageUrl = userData?.get("profileImageUrl") as? String

                if (!isLoadingProfile && !profileImageUrl.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = profileImageUrl),
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = userInitial,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Welcome back, $userName!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Statistics Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "142",
                subtitle = "Healthy\nPlants",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFF4CAF50)
            )
            StatCard(
                title = "8",
                subtitle = "At Risk\nPlants",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFF66BB6A),
                titleColor = Color(0xFFFF5252)
            )
            StatCard(
                title = "12",
                subtitle = "Scans\nToday",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFF66BB6A),
                titleColor = Color(0xFFFFD54F)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Activity Section
        Text(
            text = "Recent Activity",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Activity Items
        ActivityItem(
            icon = Icons.Default.Warning,
            iconColor = Color(0xFFFF5252),
            iconBackground = Color(0xFFFFCDD2),
            title = "Black Sigatoka Detected",
            subtitle = "Section B, Row 3 • 2 hours ago",
            indicatorColor = Color(0xFFFF5252)
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActivityItem(
            icon = Icons.Default.Check,
            iconColor = Color(0xFF4CAF50),
            iconBackground = Color(0xFFC8E6C9),
            title = "Healthy Plant Confirmed",
            subtitle = "Section A, Row 1 • 5 hours ago",
            indicatorColor = Color(0xFFFFD54F)
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActivityItem(
            icon = Icons.Default.ArrowDownward,
            iconColor = Color(0xFFFF4081),
            iconBackground = Color(0xFFF8BBD0),
            title = "Treatment Applied",
            subtitle = "Section C, Row 5 • Yesterday",
            indicatorColor = Color(0xFFFFD54F)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun NotificationContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Notifications",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun ScanContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Scan",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun HistoryContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// SettingsContent function REMOVED - it's now in SettingsPage.kt

@Composable
fun StatCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF4CAF50),
    titleColor: Color = Color.White
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun ActivityItem(
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    indicatorColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
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
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBackground, CircleShape),
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

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Indicator Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(indicatorColor, CircleShape)
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF1B5E20),
        contentColor = Color.White
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFD54F),
                selectedTextColor = Color(0xFFFFD54F),
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color(0xFF2E7D32)
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notification"
                )
            },
            label = { Text("Notification") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFD54F),
                selectedTextColor = Color(0xFFFFD54F),
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color(0xFF2E7D32)
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Scan"
                )
            },
            label = { Text("Scan") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFD54F),
                selectedTextColor = Color(0xFFFFD54F),
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color(0xFF2E7D32)
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.History,
                    contentDescription = "History"
                )
            },
            label = { Text("History") },
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFD54F),
                selectedTextColor = Color(0xFFFFD54F),
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color(0xFF2E7D32)
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings") },
            selected = selectedTab == 4,
            onClick = { onTabSelected(4) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFD54F),
                selectedTextColor = Color(0xFFFFD54F),
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color(0xFF2E7D32)
            )
        )
    }
}
