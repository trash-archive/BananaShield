package com.example.bananashield

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    paddingValues: PaddingValues,
    deepLinkScanId: String? = null,
    onDeepLinkHandled: () -> Unit = {},
    onNavigateToDetail: (ScanHistory) -> Unit, // ✅ REQUIRED - no default
    onNavigateBack: (() -> Unit)? = null
) {
    var scanHistory by remember { mutableStateOf<List<ScanHistory>>(emptyList()) }
    var filteredHistory by remember { mutableStateOf<List<ScanHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Filter/Sort/Delete states
    var sortOption by remember { mutableStateOf("Newest First") }
    var filterDisease by remember { mutableStateOf("All Diseases") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Selection and delete mode
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // ✅ Get status bar height
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density

    // ✅ Deep link handling - scrolls to scan instead of opening detail
    LaunchedEffect(deepLinkScanId) {
        deepLinkScanId?.let { scanId ->
            snapshotFlow { scanHistory }
                .filter { it.isNotEmpty() }
                .first()
                .find { it.id == scanId }
                ?.let { targetScan ->
                    val index = scanHistory.indexOf(targetScan)
                    if (index >= 0) {
                        listState.animateScrollToItem(index)
                        android.util.Log.d("HistoryContent", "✅ Deep link scrolled to: ${targetScan.diseaseName}")
                        onDeepLinkHandled()
                    }
                }
        }
    }

    // ✅ SIMPLIFIED BackHandler
    BackHandler(enabled = true) {
        when {
            isSelectionMode -> {
                isSelectionMode = false
                selectedItems = emptySet()
            }
            else -> {
                onNavigateBack?.invoke()
            }
        }
    }

    // Load scan history on first launch
    LaunchedEffect(Unit) {
        loadScanHistory(
            onSuccess = { scans ->
                scanHistory = scans
                filteredHistory = scans
                isLoading = false
            },
            onFailure = { error ->
                errorMessage = error.message
                isLoading = false
            }
        )
    }

    // Apply filters and sort
    LaunchedEffect(sortOption, filterDisease, scanHistory) {
        var result = scanHistory

        // Filter by disease
        if (filterDisease != "All Diseases") {
            result = result.filter { it.diseaseName == filterDisease }
        }

        // Sort
        result = when (sortOption) {
            "Newest First" -> result.sortedByDescending { it.timestamp }
            "Oldest First" -> result.sortedBy { it.timestamp }
            "Highest Confidence" -> result.sortedByDescending { it.confidence }
            "Lowest Confidence" -> result.sortedBy { it.confidence }
            else -> result
        }

        filteredHistory = result
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFFFEBEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Delete ${selectedItems.size} ${if (selectedItems.size == 1) "scan" else "scans"}?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    "This action cannot be undone. The selected scan history will be permanently deleted.",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        ScanHistoryHelper.deleteScans(
                            scanIds = selectedItems.toList(),
                            onSuccess = {
                                loadScanHistory(
                                    onSuccess = { scans ->
                                        scanHistory = scans
                                        selectedItems = emptySet()
                                        isSelectionMode = false
                                        showDeleteDialog = false
                                    },
                                    onFailure = { /* Handle error */ }
                                )
                            },
                            onFailure = { error ->
                                errorMessage = error.message
                                showDeleteDialog = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = Color(0xFF757575), fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(paddingValues)
    ) {
        // ✅ Header with status bar padding
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column {
                Spacer(modifier = Modifier.height(statusBarHeight.dp))

                Column(modifier = Modifier.padding(20.dp)) {
                    // ── Top row: title / selection + refresh + (optional) delete ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelectionMode) {
                                IconButton(
                                    onClick = {
                                        isSelectionMode = false
                                        selectedItems = emptySet()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel selection",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedItems.size} selected",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Scan History",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                    if (!isLoading && filteredHistory.isNotEmpty()) {
                                        Text(
                                            text = "${filteredHistory.size} ${if (filteredHistory.size == 1) "scan" else "scans"}",
                                            fontSize = 13.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelectionMode) {
                                IconButton(
                                    onClick = { if (selectedItems.isNotEmpty()) showDeleteDialog = true },
                                    enabled = selectedItems.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = if (selectedItems.isNotEmpty())
                                            Color(0xFFEF5350)
                                        else
                                            Color(0xFFBDBDBD),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            IconButton(
                                onClick = {
                                    isRefreshing = true
                                    errorMessage = null
                                    loadScanHistory(
                                        onSuccess = { scans ->
                                            scanHistory = scans
                                            isRefreshing = false
                                        },
                                        onFailure = { error ->
                                            errorMessage = error.message
                                            isRefreshing = false
                                        }
                                    )
                                }
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF2E7D32),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reload",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!isLoading && scanHistory.isNotEmpty() && !isSelectionMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = Color(0xFFF5F7FA)
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF2E7D32), Color(0xFF388E3C))
                                        )
                                    ),
                                    onClick = { showSortMenu = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sort,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = sortOption,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1B5E20),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    listOf(
                                        "Newest First",
                                        "Oldest First",
                                        "Highest Confidence",
                                        "Lowest Confidence"
                                    ).forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option,
                                                    fontWeight = if (option == sortOption)
                                                        FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                sortOption = option
                                                showSortMenu = false
                                            },
                                            leadingIcon = if (option == sortOption) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFF2E7D32)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                val hasFilter = filterDisease != "All Diseases"

                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (hasFilter)
                                            Color(0xFFFFF9C4)
                                        else
                                            Color(0xFFF5F7FA)
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = if (hasFilter) 2.dp else 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = if (hasFilter)
                                                listOf(Color(0xFFFBC02D), Color(0xFFF9A825))
                                            else
                                                listOf(Color(0xFF2E7D32), Color(0xFF388E3C))
                                        )
                                    ),
                                    onClick = { showFilterMenu = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = null,
                                            tint = if (hasFilter) Color(0xFF1B5E20) else Color(0xFF2E7D32),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (!hasFilter) "Filter" else filterDisease,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1B5E20),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = { showFilterMenu = false }
                                ) {
                                    val diseases = listOf("All Diseases") +
                                            scanHistory.map { it.diseaseName }.distinct().sorted()

                                    diseases.forEach { disease ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = disease,
                                                    fontWeight = if (disease == filterDisease)
                                                        FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                filterDisease = disease
                                                showFilterMenu = false
                                            },
                                            leadingIcon = if (disease == filterDisease) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFF2E7D32)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading scan history...",
                                color = Color(0xFF757575),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFFFFEBEE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Failed to load history",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "Unknown error",
                                fontSize = 14.sp,
                                color = Color(0xFF757575)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    isRefreshing = true
                                    errorMessage = null
                                    loadScanHistory(
                                        onSuccess = { scans ->
                                            scanHistory = scans
                                            isRefreshing = false
                                        },
                                        onFailure = { error ->
                                            errorMessage = error.message
                                            isRefreshing = false
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                filteredHistory.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(Color(0xFFF5F7FA), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (filterDisease != "All Diseases")
                                        Icons.Default.SearchOff
                                    else
                                        Icons.Default.History,
                                    contentDescription = "No scans",
                                    tint = Color(0xFFBDBDBD),
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = if (filterDisease != "All Diseases")
                                    "No results found" else "No scan history yet",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (filterDisease != "All Diseases")
                                    "Try adjusting your filter"
                                else
                                    "Start scanning plants to see results here",
                                fontSize = 14.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredHistory) { scan ->
                            ModernHistoryCard(
                                scanHistory = scan,
                                isSelected = selectedItems.contains(scan.id),
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedItems = if (selectedItems.contains(scan.id)) {
                                            selectedItems - scan.id
                                        } else {
                                            selectedItems + scan.id
                                        }
                                    } else {
                                        // ✅ USE CALLBACK TO SHOW FULLSCREEN DETAIL
                                        onNavigateToDetail(scan)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedItems = setOf(scan.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernHistoryCard(
    scanHistory: ScanHistory,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isHealthy = scanHistory.diseaseName.contains("Healthy", ignoreCase = true)
    val statusColor = if (isHealthy) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val statusBackground = if (isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF2E7D32),
                        uncheckedColor = Color(0xFFBDBDBD)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Image with status indicator
            Box(
                modifier = Modifier.size(88.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF5F5F5))
                ) {
                    if (scanHistory.imageUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = scanHistory.imageUrl),
                            contentDescription = "Scan image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "No image",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            tint = Color(0xFFBDBDBD)
                        )
                    }
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(28.dp)
                        .background(statusBackground, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scanHistory.diseaseName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = scanHistory.scientificName,
                    fontSize = 12.sp,
                    color = Color(0xFF757575),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Confidence bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(80.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFF0F0F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(scanHistory.confidence)
                                .background(
                                    when {
                                        scanHistory.confidence > 0.8f -> Color(0xFF4CAF50)
                                        scanHistory.confidence > 0.6f -> Color(0xFFFFD54F)
                                        else -> Color(0xFFEF5350)
                                    }
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(scanHistory.confidence * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            scanHistory.confidence > 0.8f -> Color(0xFF4CAF50)
                            scanHistory.confidence > 0.6f -> Color(0xFFF9A825)
                            else -> Color(0xFFEF5350)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ScanHistoryHelper.formatTimestamp(scanHistory.timestamp),
                        fontSize = 11.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }

            if (!isSelectionMode) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = Color(0xFFBDBDBD),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun loadScanHistory(
    onSuccess: (List<ScanHistory>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    ScanHistoryHelper.getUserScanHistory(
        onSuccess = onSuccess,
        onFailure = onFailure
    )
}
