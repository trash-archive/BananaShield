package com.example.bananashield

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import java.util.*

// ============================================================================
// DATA CLASSES
// ============================================================================

// ✅ FIXED: Changed isRead to read (Firestore compatible)
data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.SCAN_COMPLETE,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,  // ✅ Changed from isRead
    val relatedId: String? = null,
    val actionUrl: String? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL
)

enum class NotificationType {
    SCAN_COMPLETE,
    ADMIN_REPLY,
    LOW_CONFIDENCE_WARNING,
    DISEASE_DETECTED,
    WEEKLY_REPORT,
    SYSTEM_UPDATE,
    HEALTH_TIP
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

// ============================================================================
// NOTIFICATION CONTENT SCREEN
// ============================================================================

@Composable
fun NotificationContent(
    paddingValues: PaddingValues,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToScanDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf<AppNotification?>(null) }
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density

    BackHandler(enabled = true) {
        onNavigateBack?.invoke()
    }

    // ✅ FIXED: Use real-time listener instead of one-time get
    DisposableEffect(currentUser?.uid) {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        currentUser?.uid?.let { userId ->
            listenerRegistration = NotificationHelper.listenToNotifications(
                userId = userId,
                onUpdate = { notifs ->
                    notifications = notifs
                    isLoading = false
                }
            )
        }
        onDispose {
            listenerRegistration?.remove()
        }
    }

    val filteredNotifications = when (selectedFilter) {
        "Unread" -> notifications.filter { !it.read }  // ✅ Changed from isRead
        "Scan Results" -> notifications.filter {
            it.type == NotificationType.SCAN_COMPLETE ||
                    it.type == NotificationType.DISEASE_DETECTED ||
                    it.type == NotificationType.LOW_CONFIDENCE_WARNING
        }
        "Messages" -> notifications.filter { it.type == NotificationType.ADMIN_REPLY }
        "Tips" -> notifications.filter {
            it.type == NotificationType.HEALTH_TIP ||
                    it.type == NotificationType.WEEKLY_REPORT
        }
        else -> notifications
    }

    val unreadCount = notifications.count { !it.read }  // ✅ Changed from isRead

    // Delete single notification dialog
    showDeleteDialog?.let { notificationId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = {
                Text(
                    text = "Delete Notification",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete this notification?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        NotificationHelper.deleteNotification(notificationId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = Color(0xFF757575))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Clear all notifications dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    text = "Clear All Notifications",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This will permanently delete all notifications. Are you sure?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentUser?.uid?.let { userId ->
                            NotificationHelper.clearAllNotifications(userId)
                        }
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350)
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = Color(0xFF9E9E9E))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    showMessageDialog?.let { notification ->
        AdminReplyDialog(
            notification = notification,
            onDismiss = { showMessageDialog = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(paddingValues)
    ) {
        // TOP BAR WITH STATUS BAR PADDING
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column {
                Spacer(modifier = Modifier.height(statusBarHeight.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifications",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            if (unreadCount > 0) {
                                Text(
                                    text = "$unreadCount unread",
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }

                        if (notifications.isNotEmpty()) {
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = Color(0xFF757575)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    if (unreadCount > 0) {
                                        DropdownMenuItem(
                                            text = { Text("Mark all as read") },
                                            onClick = {
                                                NotificationHelper.markAllAsRead(currentUser?.uid ?: "")
                                                showMenu = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.DoneAll,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32)
                                                )
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Clear all", color = Color(0xFFEF5350)) },
                                        onClick = {
                                            showClearAllDialog = true
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DeleteSweep,
                                                contentDescription = null,
                                                tint = Color(0xFFEF5350)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf("All", "Unread", "Scan Results", "Messages")
                        items(filters.size) { index ->
                            val filter = filters[index]
                            val count = when (filter) {
                                "All" -> notifications.size
                                "Unread" -> unreadCount
                                "Scan Results" -> notifications.count {
                                    it.type == NotificationType.SCAN_COMPLETE ||
                                            it.type == NotificationType.DISEASE_DETECTED ||
                                            it.type == NotificationType.LOW_CONFIDENCE_WARNING
                                }
                                "Messages" -> notifications.count { it.type == NotificationType.ADMIN_REPLY }
                                "Tips" -> notifications.count {
                                    it.type == NotificationType.HEALTH_TIP ||
                                            it.type == NotificationType.WEEKLY_REPORT
                                }
                                else -> 0
                            }

                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = {
                                    Text(
                                        text = if (count > 0) "$filter ($count)" else filter,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedFilter == filter) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFE8F5E9),
                                    labelColor = Color(0xFF2E7D32)
                                ),
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // Content area
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else if (filteredNotifications.isEmpty()) {
            EmptyNotificationsView(filter = selectedFilter)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(
                    items = filteredNotifications,
                    key = { it.id }
                ) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            android.util.Log.d("NotificationContent", "🔔 Clicked notification: ${notification.id}, read: ${notification.read}")
                            if (!notification.read) {
                                android.util.Log.d("NotificationContent", "✅ Marking as read: ${notification.id}")
                                NotificationHelper.markAsRead(notification.id)
                            }

                            // ✅ Show dialog for admin replies
                            if (notification.type == NotificationType.ADMIN_REPLY) {
                                android.util.Log.d("NotificationContent", "📱 Opening dialog for admin reply: ${notification.id}")
                                showMessageDialog = notification
                            }
                            // Navigate for scan results
                            else if (notification.relatedId != null &&
                                (notification.type == NotificationType.SCAN_COMPLETE ||
                                        notification.type == NotificationType.DISEASE_DETECTED ||
                                        notification.type == NotificationType.LOW_CONFIDENCE_WARNING)) {
                                onNavigateToScanDetail(notification.relatedId)
                            }
                        },
                        onDelete = {
                            showDeleteDialog = notification.id
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, iconColor, bgColor) = when (notification.type) {
        NotificationType.SCAN_COMPLETE -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF4CAF50),
            Color(0xFFE8F5E9)
        )
        NotificationType.ADMIN_REPLY -> Triple(
            Icons.Default.Reply,
            Color(0xFF2196F3),
            Color(0xFFE3F2FD)
        )
        NotificationType.LOW_CONFIDENCE_WARNING -> Triple(
            Icons.Default.Warning,
            Color(0xFFFF9800),
            Color(0xFFFFF3E0)
        )
        NotificationType.DISEASE_DETECTED -> Triple(
            Icons.Default.MedicalServices,
            Color(0xFFEF5350),
            Color(0xFFFFEBEE)
        )
        NotificationType.WEEKLY_REPORT -> Triple(
            Icons.Default.Assessment,
            Color(0xFF9C27B0),
            Color(0xFFF3E5F5)
        )
        NotificationType.HEALTH_TIP -> Triple(
            Icons.Default.Lightbulb,
            Color(0xFFFBC02D),
            Color(0xFFFFF9C4)
        )
        NotificationType.SYSTEM_UPDATE -> Triple(
            Icons.Default.Info,
            Color(0xFF607D8B),
            Color(0xFFECEFF1)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.read) Color.White else Color(0xFFF5F5F5)  // ✅ Changed from isRead
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.read) 2.dp else 1.dp  // ✅ Changed from isRead
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (!notification.read) bgColor else bgColor.copy(alpha = 0.6f)),  // ✅ Changed from isRead
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (!notification.read) iconColor else iconColor.copy(alpha = 0.7f),  // ✅ Changed from isRead
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        fontSize = 15.sp,
                        fontWeight = if (!notification.read) FontWeight.Bold else FontWeight.SemiBold,  // ✅ Changed from isRead
                        color = if (!notification.read) Color(0xFF1B5E20) else Color(0xFF757575),  // ✅ Changed from isRead
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.read) {  // ✅ Changed from isRead
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF5350))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = if (!notification.read) Color(0xFF757575) else Color(0xFF9E9E9E),  // ✅ Changed from isRead
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTimeAgo(notification.timestamp),
                        fontSize = 11.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyNotificationsView(filter: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = when (filter) {
                    "Scan Results" -> Icons.Default.QrCodeScanner
                    "Messages" -> Icons.Default.Message
                    "Tips" -> Icons.Default.Lightbulb
                    else -> Icons.Default.Notifications
                },
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (filter) {
                    "Unread" -> "No unread notifications"
                    "Scan Results" -> "No scan notifications"
                    "Messages" -> "No message notifications"
                    "Tips" -> "No tips yet"
                    else -> "No notifications yet"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (filter) {
                    "Scan Results" -> "Scan results will appear here"
                    "Messages" -> "Admin replies will appear here"
                    "Tips" -> "Plant care tips will appear here"
                    else -> "You'll see updates about your scans and messages here"
                },
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// NOTIFICATION HELPER
// ============================================================================
object NotificationHelper {
    private val db = Firebase.firestore
    private const val COLLECTION = "notifications"

    // ✅ CRITICAL FIX: In-memory cache to prevent race condition duplicates
    private val pendingNotifications = mutableSetOf<String>()
    private val notificationLock = Any()

    // ✅ FIXED: Real-time listener for notifications
    fun listenToNotifications(
        userId: String,
        onUpdate: (List<AppNotification>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection(COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotificationHelper", "Listen failed", error)
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationHelper", "Error parsing notification", e)
                        null
                    }
                } ?: emptyList()

                android.util.Log.d("NotificationHelper", "✅ Loaded ${notifications.size} notifications")
                onUpdate(notifications)
            }
    }

    fun getUnreadCount(
        userId: String,
        onSuccess: (Int) -> Unit
    ) {
        db.collection(COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { snapshot ->
                android.util.Log.d("NotificationHelper", "Unread count: ${snapshot.size()}")
                onSuccess(snapshot.size())
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "Failed to get unread count", e)
                onSuccess(0)
            }
    }

    private fun createNotification(
        userId: String,
        type: NotificationType,
        title: String,
        message: String,
        relatedId: String? = null,
        actionUrl: String? = null,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ) {
        val notification = hashMapOf(
            "userId" to userId,
            "type" to type.name,
            "title" to title,
            "message" to message,
            "relatedId" to relatedId,
            "actionUrl" to actionUrl,
            "priority" to priority.name,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )

        db.collection(COLLECTION)
            .add(notification)
            .addOnSuccessListener {
                android.util.Log.d("NotificationHelper", "✅ Notification created: $title")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "❌ Failed to create notification", e)
            }
    }

    fun markAsRead(notificationId: String) {
        android.util.Log.d("NotificationHelper", "📝 Marking as read: $notificationId")
        db.collection(COLLECTION)
            .document(notificationId)
            .update("read", true)
            .addOnSuccessListener {
                android.util.Log.d("NotificationHelper", "✅ Successfully marked as read: $notificationId")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "❌ Failed to mark as read", e)
            }
    }

    fun markAllAsRead(userId: String) {
        android.util.Log.d("NotificationHelper", "📝 Marking all as read for user: $userId")
        db.collection(COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    android.util.Log.d("NotificationHelper", "No unread notifications to mark")
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "read", true)
                }

                batch.commit()
                    .addOnSuccessListener {
                        android.util.Log.d("NotificationHelper", "✅ Marked ${snapshot.size()} notifications as read")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("NotificationHelper", "❌ Failed to mark all as read", e)
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "❌ Failed to query notifications", e)
            }
    }

    fun deleteNotification(notificationId: String) {
        db.collection(COLLECTION)
            .document(notificationId)
            .delete()
            .addOnSuccessListener {
                android.util.Log.d("NotificationHelper", "✅ Notification deleted: $notificationId")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "❌ Failed to delete notification", e)
            }
    }

    fun clearAllNotifications(userId: String) {
        db.collection(COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        android.util.Log.d("NotificationHelper", "✅ All notifications cleared")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("NotificationHelper", "❌ Failed to clear all notifications", e)
                    }
            }
    }

    // ✅ CRITICAL FIX: Synchronize access with in-memory cache
    fun notifyScanComplete(
        userId: String,
        scanId: String,
        diseaseName: String,
        confidence: Float
    ) {
        // ✅ Step 1: Check in-memory cache FIRST (instant check)
        synchronized(notificationLock) {
            if (pendingNotifications.contains(scanId)) {
                android.util.Log.d("NotificationHelper", "⚠️ Notification already being created for scanId: $scanId (in-memory check)")
                return
            }

            // ✅ Mark as pending immediately
            pendingNotifications.add(scanId)
            android.util.Log.d("NotificationHelper", "🔒 Locked scanId: $scanId in memory")
        }

        // ✅ Step 2: Double-check with Firestore (async check)
        android.util.Log.d("NotificationHelper", "🔍 Checking Firestore for existing notification: scanId=$scanId")

        db.collection(COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("relatedId", scanId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    android.util.Log.d("NotificationHelper", "✅ No existing notification in Firestore, creating new one")

                    val isHealthy = diseaseName.contains("Healthy", ignoreCase = true)
                    val (type, title, message, priority) = when {
                        !isHealthy && confidence >= 0.6f -> {
                            Tuple4(
                                NotificationType.DISEASE_DETECTED,
                                "Disease Detected!",
                                "$diseaseName detected with ${(confidence * 100).toInt()}% confidence. Check treatment recommendations.",
                                NotificationPriority.HIGH
                            )
                        }
                        confidence < 0.6f -> {
                            Tuple4(
                                NotificationType.LOW_CONFIDENCE_WARNING,
                                "Low Confidence Scan",
                                "Scan confidence is ${(confidence * 100).toInt()}%. Consider rescanning for better accuracy.",
                                NotificationPriority.NORMAL
                            )
                        }
                        else -> {
                            Tuple4(
                                NotificationType.SCAN_COMPLETE,
                                "Scan Complete",
                                "Your plant scan is complete. Result: $diseaseName",
                                NotificationPriority.NORMAL
                            )
                        }
                    }

                    createNotification(
                        userId = userId,
                        type = type,
                        title = title,
                        message = message,
                        relatedId = scanId,
                        actionUrl = "history/$scanId",
                        priority = priority
                    )
                } else {
                    android.util.Log.d("NotificationHelper", "⚠️ Notification already exists in Firestore for scan: $scanId (found ${snapshot.size()} notifications)")

                    // ✅ Remove from pending since we found it in Firestore
                    synchronized(notificationLock) {
                        pendingNotifications.remove(scanId)
                    }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "❌ Failed to check for existing notifications", e)

                // ✅ Remove from pending on error
                synchronized(notificationLock) {
                    pendingNotifications.remove(scanId)
                }
            }

        // ✅ Step 3: Auto-cleanup after 10 seconds (in case Firestore fails silently)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            synchronized(notificationLock) {
                if (pendingNotifications.remove(scanId)) {
                    android.util.Log.d("NotificationHelper", "🧹 Auto-cleaned pending scanId: $scanId after 10s")
                }
            }
        }, 10000) // 10 seconds cleanup timeout
    }

// Add this to your NotificationHelper object in NotificationContent.kt

// ============================================================================
// REAL-TIME LISTENER FOR CONTACT MESSAGE REPLIES
// ============================================================================

    /**
     * Listen for admin replies to user's contact messages
     * This will automatically create notifications when admin replies
     */
    fun listenForAdminReplies(userId: String): com.google.firebase.firestore.ListenerRegistration {
        val db = Firebase.firestore

        return db.collection("contact_messages")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotificationHelper", "Listen for replies failed", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                        val doc = change.document
                        val adminReply = doc.getString("admin_reply")
                        val messageId = doc.id

                        // Check if admin_reply was just added (not empty)
                        if (!adminReply.isNullOrBlank()) {
                            android.util.Log.d("NotificationHelper", "📩 Admin reply detected for message: $messageId")

                            // Check if notification already exists for this reply
                            checkAndCreateReplyNotification(
                                userId = userId,
                                messageId = messageId,
                                adminReply = adminReply,
                                subject = doc.getString("subject") ?: "Your Inquiry"
                            )
                        }
                    }
                }
            }
    }

    /**
     * Check if notification exists before creating one
     * Prevents duplicate notifications for the same reply
     */
    private fun checkAndCreateReplyNotification(
        userId: String,
        messageId: String,
        adminReply: String,
        subject: String
    ) {
        val db = Firebase.firestore

        db.collection(COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("relatedId", messageId)
            .whereEqualTo("type", NotificationType.ADMIN_REPLY.name)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    android.util.Log.d("NotificationHelper", "✅ Creating new admin reply notification")

                    createNotification(
                        userId = userId,
                        type = NotificationType.ADMIN_REPLY,
                        title = "Admin Reply: $subject",
                        message = adminReply,  // ✅ Full reply message shown in notification
                        relatedId = messageId,
                        actionUrl = null,  // ✅ No navigation needed
                        priority = NotificationPriority.HIGH
                    )
                } else {
                    android.util.Log.d("NotificationHelper", "ℹ️ Notification already exists for this reply")
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "❌ Failed to check existing notifications", e)
            }
    }

    fun notifyAdminReply(
        userId: String,
        inquiryId: String,
        replyPreview: String
    ) {
        createNotification(
            userId = userId,
            type = NotificationType.ADMIN_REPLY,
            title = "Admin Replied to Your Inquiry",
            message = replyPreview.take(80) + if (replyPreview.length > 80) "..." else "",
            relatedId = inquiryId,
            actionUrl = "contact/$inquiryId",
            priority = NotificationPriority.HIGH
        )
    }

    fun notifyWeeklyReport(
        userId: String,
        totalScans: Int,
        healthyCount: Int,
        diseasedCount: Int
    ) {
        createNotification(
            userId = userId,
            type = NotificationType.WEEKLY_REPORT,
            title = "Weekly Health Report 📊",
            message = "This week: $totalScans scans • $healthyCount healthy • $diseasedCount diseased plants",
            priority = NotificationPriority.NORMAL
        )
    }

    fun notifyHealthTip(userId: String, tip: String) {
        createNotification(
            userId = userId,
            type = NotificationType.HEALTH_TIP,
            title = "Plant Care Tip 💡",
            message = tip,
            priority = NotificationPriority.LOW
        )
    }

    private data class Tuple4<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}

@Composable
fun AdminReplyDialog(
    notification: AppNotification,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2196F3),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Reply",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "BananaShield Support",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Content - Scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // Subject
                    Text(
                        text = "Subject",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = notification.title.removePrefix("Admin Reply: "),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider
                    Divider(
                        color = Color(0xFFE0E0E0),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Message Label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Message",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full Message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Text(
                            text = notification.message,
                            fontSize = 15.sp,
                            color = Color(0xFF212121),
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timestamp
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                                .format(Date(notification.timestamp)),
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }

                // Footer with action button
                Divider(
                    color = Color(0xFFE0E0E0),
                    thickness = 1.dp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "Got it",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
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
