package com.example.bananashield

import android.app.Activity
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomePage(
    initialTab: Int? = null,
    deepLinkScanId: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    val userName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "User"
    val userInitial = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    var selectedTab by remember { mutableStateOf(initialTab ?: 0) }
    var showProfile by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showFAQ by remember { mutableStateOf(false) }
    var showContactUs by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    // Handle deep link navigation
    LaunchedEffect(initialTab) {
        initialTab?.let {
            selectedTab = it
            onDeepLinkHandled()
        }
    }

    // Set status bar color to match header
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(selectedTab) {
            val window = (view.context as Activity).window
            window.statusBarColor = when (selectedTab) {
                0 -> android.graphics.Color.parseColor("#2E7D32")
                2 -> android.graphics.Color.parseColor("#000000") // Black for scan
                else -> android.graphics.Color.parseColor("#F5F7FA")
            }

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                selectedTab != 0 && selectedTab != 2
        }
    }

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
                    // Hide bottom navigation when on scan tab (index 2)
                    if (selectedTab != 2) {
                        BottomNavigationBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                }
            ) { paddingValues ->
                // Provide paddingValues(0.dp) for scan screen for fullscreen camera
                val contentPadding = if (selectedTab == 2) PaddingValues(0.dp) else paddingValues

                when (selectedTab) {
                    0 -> HomeContent(
                        paddingValues = contentPadding,
                        userName = userName,
                        userInitial = userInitial,
                        onProfileClick = { showProfile = true },
                        onScanClick = { selectedTab = 2 },
                        onHistoryClick = { selectedTab = 3 }
                    )
                    1 -> NotificationContent(
                        paddingValues = contentPadding,
                        onNavigateBack = { selectedTab = 0 }
                    )
                    2 -> ScanContent(
                        contentPadding,
                        onNavigateBack = { selectedTab = 0 }
                    )
                    3 -> HistoryContent(
                        paddingValues = contentPadding,
                        deepLinkScanId = deepLinkScanId,
                        onDeepLinkHandled = onDeepLinkHandled
                    )
                    4 -> SettingsContent(
                        paddingValues = contentPadding,
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
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var scanHistory by remember { mutableStateOf<List<ScanHistory>>(emptyList()) }
    var isLoadingScans by remember { mutableStateOf(true) }
    var unreadNotificationCount by remember { mutableStateOf(0) }

    // Get status bar height
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density

    // Load user profile
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

    // Load scan history for statistics
    LaunchedEffect(Unit) {
        ScanHistoryHelper.getUserScanHistory(
            onSuccess = { scans ->
                scanHistory = scans
                isLoadingScans = false
            },
            onFailure = {
                isLoadingScans = false
            }
        )
    }

    // Load unread notification count
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            NotificationHelper.getUnreadCount(
                userId = userId,
                onSuccess = { count ->
                    unreadNotificationCount = count
                }
            )
        }
    }

    // Calculate statistics
    val totalScans = scanHistory.size
    val healthyScans = scanHistory.count { it.diseaseName.contains("Healthy", ignoreCase = true) }
    val diseasedScans = totalScans - healthyScans

    // Get today's scans
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val todayStart = calendar.timeInMillis
    val todayScans = scanHistory.count { it.timestamp >= todayStart }

    // Get recent 3 scans for activity
    val recentScans = scanHistory.take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2E7D32),
                            Color(0xFF388E3C)
                        )
                    )
                )
        ) {
            Column {
                Spacer(modifier = Modifier.height(statusBarHeight.dp + 5.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile picture
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
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
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Greeting text
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, $userName! 👋",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Let's keep your plants healthy today",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Notification icon with badge
                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { /* Notification action */ },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Badge
                        if (unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .background(Color(0xFFFDD835), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(54.dp))
            }

            // Curved bottom edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Color(0xFFF5F7FA),
                        RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Statistics Cards with labels
        Text(
            text = "Overview",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20),
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoadingScans) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernStatCard(
                    icon = Icons.Default.CheckCircle,
                    value = healthyScans.toString(),
                    label = "Healthy",
                    description = "Plants",
                    backgroundColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                ModernStatCard(
                    icon = Icons.Default.Warning,
                    value = diseasedScans.toString(),
                    label = "Diseased",
                    description = "Plants",
                    backgroundColor = Color(0xFFFFEBEE),
                    iconColor = Color(0xFFEF5350),
                    modifier = Modifier.weight(1f)
                )
                ModernStatCard(
                    icon = Icons.Default.Today,
                    value = todayScans.toString(),
                    label = "Scans",
                    description = "Today",
                    backgroundColor = Color(0xFFFFF9C4),
                    iconColor = Color(0xFFFBC02D),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions
        Text(
            text = "Quick Actions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20),
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.QrCodeScanner,
                label = "New Scan",
                backgroundColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f),
                onClick = onScanClick
            )
            QuickActionCard(
                icon = Icons.Default.History,
                label = "History",
                backgroundColor = Color(0xFF1976D2),
                modifier = Modifier.weight(1f),
                onClick = onHistoryClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Activity
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )

            if (recentScans.isNotEmpty()) {
                TextButton(onClick = onHistoryClick) {
                    Text(
                        text = "View All",
                        fontSize = 13.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoadingScans) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else if (recentScans.isEmpty()) {
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
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No scans yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start scanning to track your plants' health",
                        fontSize = 13.sp,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            recentScans.forEach { scan ->
                ModernActivityItem(
                    scan = scan,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Keep all other composables (ModernStatCard, QuickActionCard, etc.) as they were...
@Composable
fun ModernStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    description: String,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(backgroundColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1B5E20)
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (backgroundColor == Color(0xFFFBC02D)) Color(0xFF1B5E20) else Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (backgroundColor == Color(0xFFFBC02D)) Color(0xFF1B5E20) else Color.White
            )
        }
    }
}

@Composable
fun ModernActivityItem(
    scan: ScanHistory,
    modifier: Modifier = Modifier
) {
    val isHealthy = scan.diseaseName.contains("Healthy", ignoreCase = true)
    val iconColor = if (isHealthy) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val iconBackground = if (isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val icon = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning

    val timeAgo = getTimeAgo(scan.timestamp)

    Card(
        modifier = modifier.fillMaxWidth(),
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

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = scan.diseaseName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeAgo,
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
            }

            Text(
                text = "${(scan.confidence * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        }
    }
}

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    var unreadCount by remember { mutableStateOf(0) }

    LaunchedEffect(currentUser?.uid, selectedTab) {
        currentUser?.uid?.let { userId ->
            NotificationHelper.getUnreadCount(
                userId = userId,
                onSuccess = { count ->
                    unreadCount = count
                }
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = Color(0xFF2E7D32),
            modifier = Modifier.height(80.dp)
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home", fontSize = 11.sp) },
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2E7D32),
                    selectedTextColor = Color(0xFF2E7D32),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                icon = {
                    Box {
                        Icon(Icons.Default.Notifications, contentDescription = "Notification")

                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-4).dp)
                                    .background(Color(0xFFEF5350), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                label = { Text("Alerts", fontSize = 11.sp) },
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2E7D32),
                    selectedTextColor = Color(0xFF2E7D32),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .offset(y = (-8).dp)
                            .shadow(8.dp, CircleShape)
                            .background(Color(0xFF2E7D32), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                label = { },
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color(0xFF2E7D32),
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                label = { Text("History", fontSize = 11.sp) },
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2E7D32),
                    selectedTextColor = Color(0xFF2E7D32),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings", fontSize = 11.sp) },
                selected = selectedTab == 4,
                onClick = { onTabSelected(4) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2E7D32),
                    selectedTextColor = Color(0xFF2E7D32),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
