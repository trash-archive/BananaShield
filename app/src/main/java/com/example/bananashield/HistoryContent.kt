package com.example.bananashield

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(paddingValues: PaddingValues) {
    var scanHistory by remember { mutableStateOf<List<ScanHistory>>(emptyList()) }
    var filteredHistory by remember { mutableStateOf<List<ScanHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedScan by remember { mutableStateOf<ScanHistory?>(null) }

    // Filter/Sort/Delete states
    var sortOption by remember { mutableStateOf("Newest First") }
    var filterDisease by remember { mutableStateOf("All Diseases") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Selection and delete mode
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFF5252)
                )
            },
            title = {
                Text(
                    text = "Delete ${selectedItems.size} ${if (selectedItems.size == 1) "scan" else "scans"}?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This action cannot be undone. The selected scan history will be permanently deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Delete selected items
                        ScanHistoryHelper.deleteScans(
                            scanIds = selectedItems.toList(),
                            onSuccess = {
                                // Refresh the list
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
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show detailed view if a scan is selected (and not in selection mode)
    if (selectedScan != null && !isSelectionMode) {
        HistoryDetailScreen(
            scanHistory = selectedScan!!,
            onBack = { selectedScan = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
            .padding(paddingValues)
    ) {
        // Header with conditional action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    IconButton(onClick = {
                        isSelectionMode = false
                        selectedItems = emptySet()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel selection",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedItems.size} selected",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Scan History",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Action buttons
            if (isSelectionMode) {
                Row {
                    // Select All / Deselect All
                    TextButton(
                        onClick = {
                            selectedItems = if (selectedItems.size == filteredHistory.size) {
                                emptySet()
                            } else {
                                filteredHistory.map { it.id }.toSet()
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedItems.size == filteredHistory.size) "Deselect All" else "Select All",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = selectedItems.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (selectedItems.isNotEmpty()) Color(0xFFFF5252) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Filter and Sort Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sort Button
            Box(modifier = Modifier.weight(1f)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
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
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sortOption,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    listOf("Newest First", "Oldest First", "Highest Confidence", "Lowest Confidence").forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                sortOption = option
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (option == sortOption) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Filter Button
            Box(modifier = Modifier.weight(1f)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (filterDisease != "All Diseases")
                            Color(0xFFFFD54F)
                        else
                            Color.White.copy(alpha = 0.15f)
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
                            tint = if (filterDisease != "All Diseases") Color(0xFF1B5E20) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (filterDisease == "All Diseases") "All Diseases" else filterDisease,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (filterDisease != "All Diseases") Color(0xFF1B5E20) else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    val uniqueDiseases = listOf("All Diseases") + scanHistory.map { it.diseaseName }.distinct().sorted()
                    uniqueDiseases.forEach { disease ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = disease,
                                    fontWeight = if (disease == filterDisease) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                filterDisease = disease
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (disease == filterDisease) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                                color = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading scan history...",
                                color = Color.White,
                                fontSize = 16.sp
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
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Failed to load history",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "Unknown error",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
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
                                    containerColor = Color.White
                                )
                            ) {
                                Text("Retry", color = Color(0xFF2E7D32))
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
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = "No scans",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (filterDisease != "All Diseases")
                                    "No results found" else "No scan history yet",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (filterDisease != "All Diseases")
                                    "Try adjusting your filter"
                                else
                                    "Start scanning plants to see results here",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filteredHistory.size} ${if (filteredHistory.size == 1) "scan" else "scans"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                IconButton(
                                    onClick = {
                                        isRefreshing = true
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
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        items(filteredHistory) { scan ->
                            HistoryCard(
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
                                        selectedScan = scan
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
fun HistoryCard(
    scanHistory: ScanHistory,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
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
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF2E7D32),
                        uncheckedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                            .padding(20.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    scanHistory.confidence > 0.8f -> Color(0xFF4CAF50)
                                    scanHistory.confidence > 0.6f -> Color(0xFFFFD54F)
                                    else -> Color(0xFFFF5252)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(scanHistory.confidence * 100).toInt()}% confidence",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = ScanHistoryHelper.formatTimestamp(scanHistory.timestamp),
                    fontSize = 11.sp,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }

            if (!isSelectionMode) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = Color.Gray,
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
