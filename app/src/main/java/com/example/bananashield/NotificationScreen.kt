package com.example.bananashield

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// DATA CLASSES
// ============================================================================

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.SCAN_COMPLETE,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
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
fun NotificationContent(paddingValues: PaddingValues) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            NotificationHelper.getUserNotifications(
                userId = userId,
                onSuccess = { notifs ->
                    notifications = notifs
                    isLoading = false
                },
                onFailure = {
                    isLoading = false
                }
            )
        }
    }

    val filteredNotifications = when (selectedFilter) {
        "Unread" -> notifications.filter { !it.isRead }
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

    val unreadCount = notifications.count { !it.isRead }

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
                        notifications = notifications.filter { it.id != notificationId }
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
                            notifications = emptyList()
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
                    Text("Cancel", color = Color(0xFF757575))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(paddingValues)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
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
                                            notifications = notifications.map { it.copy(isRead = true) }
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

                // ✅ IMPROVED: Scrollable filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("All", "Unread", "Scan Results", "Messages", "Tips")

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
                            if (!notification.isRead) {
                                NotificationHelper.markAsRead(notification.id)
                                notifications = notifications.map {
                                    if (it.id == notification.id) it.copy(isRead = true) else it
                                }
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) Color.White else Color(0xFFFAFAFA)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.isRead) 2.dp else 1.dp
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
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
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
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFF1B5E20),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
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
                    color = Color(0xFF757575),
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

    fun getUserNotifications(
        userId: String,
        onSuccess: (List<AppNotification>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val notifications = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationHelper", "Error parsing notification", e)
                        null
                    }
                }
                onSuccess(notifications)
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("NotificationHelper", "Failed to get notifications", exception)
                onFailure(exception)
            }
    }

    // ✅ NEW: Get unread count for badge
    fun getUnreadCount(
        userId: String,
        onSuccess: (Int) -> Unit
    ) {
        db.collection(COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.size())
            }
            .addOnFailureListener {
                android.util.Log.e("NotificationHelper", "Failed to get unread count")
                onSuccess(0)
            }
    }

    fun createNotification(
        userId: String,
        type: NotificationType,
        title: String,
        message: String,
        relatedId: String? = null,
        actionUrl: String? = null,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ) {
        val notification = AppNotification(
            userId = userId,
            type = type,
            title = title,
            message = message,
            relatedId = relatedId,
            actionUrl = actionUrl,
            priority = priority,
            timestamp = System.currentTimeMillis(),
            isRead = false
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
        db.collection(COLLECTION)
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                android.util.Log.d("NotificationHelper", "Marked as read: $notificationId")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "Failed to mark as read", e)
            }
    }

    fun markAllAsRead(userId: String) {
        db.collection(COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit()
                    .addOnSuccessListener {
                        android.util.Log.d("NotificationHelper", "All notifications marked as read")
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "Failed to mark all as read", e)
            }
    }

    fun deleteNotification(notificationId: String) {
        db.collection(COLLECTION)
            .document(notificationId)
            .delete()
            .addOnSuccessListener {
                android.util.Log.d("NotificationHelper", "Notification deleted: $notificationId")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "Failed to delete notification", e)
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
                        android.util.Log.d("NotificationHelper", "All notifications cleared")
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotificationHelper", "Failed to clear all notifications", e)
            }
    }

    fun notifyScanComplete(
        userId: String,
        scanId: String,
        diseaseName: String,
        confidence: Float
    ) {
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
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

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
