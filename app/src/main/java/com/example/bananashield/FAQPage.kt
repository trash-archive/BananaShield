package com.example.bananashield

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQPage(onNavigateBack: () -> Unit) {
    var showDetectionAccuracy by remember { mutableStateOf(false) }

    if (showDetectionAccuracy) {
        DetectionAccuracyPage(onNavigateBack = { showDetectionAccuracy = false })
    } else {
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
                    text = "FAQ and Support",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "BananaShield can detect 3 major diseases",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // What diseases can be detected?
            Text(
                text = "What diseases can be detected?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Disease Cards
            DiseaseCard(
                title = "Black Sigatoka",
                scientificName = "Mycosphaerella fijiensis",
                accuracy = "96%",
                description = "Dark streaks on leaves, yellow halos"
            )

            Spacer(modifier = Modifier.height(12.dp))

            DiseaseCard(
                title = "Banana Bunchy Top",
                scientificName = "BBTV - Viral Disease",
                accuracy = "93%",
                description = "Stunted growth, bunched leaves at top"
            )

            Spacer(modifier = Modifier.height(12.dp))

            DiseaseCard(
                title = "Fusarium Wilt (TR4)",
                scientificName = "Panama Disease - Tropical Race 4",
                accuracy = "92%",
                description = "Wilting, yellowing leaves, plant death"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // FAQ Items
            FAQItem(
                question = "How do I scan a plant?",
                answer = "To scan a plant, tap the Scan button, position the leaf within the frame, and capture the image. Make sure the leaf is well-lit and in focus for best results."
            )

            Spacer(modifier = Modifier.height(12.dp))

            FAQItem(
                question = "How accurate is the detection?",
                answer = "",
                isClickable = true,
                onClick = { showDetectionAccuracy = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DiseaseCard(
    title: String,
    scientificName: String,
    accuracy: String,
    description: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Row - Always Visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Warning Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5252)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Disease",
                        tint = Color.White,
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
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = scientificName,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Accuracy: $accuracy",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD54F)
                    )
                }

                // Expand/Collapse Icon
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Expanded Content
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Common Symptoms Section
                    Text(
                        text = "Common Symptoms",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    when (title) {
                        "Black Sigatoka" -> {
                            SymptomsList(
                                symptoms = listOf(
                                    "Dark brown to black streaks on leaves",
                                    "Yellow halos around infected areas",
                                    "Premature leaf death and drying",
                                    "Reduced fruit production"
                                )
                            )
                        }
                        "Banana Bunchy Top" -> {
                            SymptomsList(
                                symptoms = listOf(
                                    "Dark brown to black streaks on leaves",
                                    "Yellow halos around infected areas",
                                    "Premature leaf death and drying",
                                    "Reduced fruit production"
                                )
                            )
                        }
                        "Fusarium Wilt (TR4)" -> {
                            SymptomsList(
                                symptoms = listOf(
                                    "Yellowing and wilting of older leaves",
                                    "Vascular discoloration (reddish-brown)",
                                    "Leaf collapse and plant death",
                                    "Soil-borne, can persist for decades"
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Prevention Tips Section
                    Text(
                        text = "Prevention Tips",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    when (title) {
                        "Black Sigatoka" -> {
                            PreventionTipsList(
                                tips = listOf(
                                    "Plant resistant varieties when available",
                                    "Maintain proper spacing for air circulation",
                                    "Regular monitoring and early detection",
                                    "Apply preventive fungicide sprays"
                                )
                            )
                        }
                        "Banana Bunchy Top" -> {
                            PreventionTipsList(
                                tips = listOf(
                                    "Plant resistant varieties when available",
                                    "Maintain proper spacing for air circulation",
                                    "Regular monitoring and early detection",
                                    "Apply preventive fungicide sprays"
                                )
                            )
                        }
                        "Fusarium Wilt (TR4)" -> {
                            PreventionTipsList(
                                tips = listOf(
                                    "Use resistant banana varieties",
                                    "Clean tools and equipment thoroughly",
                                    "Prevent soil movement from infected areas",
                                    "Report suspected cases immediately"
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SymptomsList(symptoms: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E7D32)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            symptoms.forEach { symptom ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = symptom,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PreventionTipsList(tips: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E7D32)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FAQItem(
    question: String,
    answer: String,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable {
                if (isClickable) {
                    onClick()
                } else {
                    expanded = !expanded
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (isClickable) Icons.Default.ChevronRight else {
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore
                    },
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White
                )
            }

            if (!isClickable) {
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        )
                    ) {
                        Text(
                            text = answer,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
