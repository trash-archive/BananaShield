package com.example.bananashield

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class BBTVVerdict { HIGH, MODERATE, LOW }

data class BBTVQuestionnaireResult(
    val verdict: BBTVVerdict,
    val score: Int,
    val streakAnswer: String,
    val timelineAnswer: String,
    val spreadAnswer: String,
    val aphidAnswer: String
)

@Composable
fun BBTVQuestionnaireScreen(
    onComplete: (BBTVQuestionnaireResult) -> Unit,
    onAbort: () -> Unit
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density) / density.density
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density) / density.density

    var currentStep by remember { mutableStateOf(0) }
    var streakAnswer by remember { mutableStateOf("") }
    var timelineAnswer by remember { mutableStateOf("") }
    var spreadAnswer by remember { mutableStateOf("") }
    var aphidAnswer by remember { mutableStateOf("") }

    BackHandler {
        if (currentStep > 0) currentStep-- else onAbort()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // Header
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
                    IconButton(onClick = { if (currentStep > 0) currentStep-- else onAbort() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BBTV Verification",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Question ${currentStep + 1} of 4",
                            fontSize = 13.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }

                // Progress bar
                LinearProgressIndicator(
                    progress = (currentStep + 1) / 4f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFF2E7D32),
                    trackColor = Color(0xFFE0E0E0)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Info banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Answer 4 quick questions to help us verify if this is actually BBTV or just a stressed plant.",
                        fontSize = 13.sp,
                        color = Color(0xFF2E7D32),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (currentStep) {
                0 -> QuestionOne(
                    selected = streakAnswer,
                    onSelect = { streakAnswer = it }
                )
                1 -> QuestionTwo(
                    selected = timelineAnswer,
                    onSelect = { timelineAnswer = it }
                )
                2 -> QuestionThree(
                    selected = spreadAnswer,
                    onSelect = { spreadAnswer = it }
                )
                3 -> QuestionFour(
                    selected = aphidAnswer,
                    onSelect = { aphidAnswer = it }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Next / Submit button
            val canProceed = when (currentStep) {
                0 -> streakAnswer.isNotEmpty()
                1 -> timelineAnswer.isNotEmpty()
                2 -> spreadAnswer.isNotEmpty()
                3 -> aphidAnswer.isNotEmpty()
                else -> false
            }

            Button(
                onClick = {
                    if (currentStep < 3) {
                        currentStep++
                    } else {
                        val result = computeVerdict(streakAnswer, timelineAnswer, spreadAnswer, aphidAnswer)
                        onComplete(result)
                    }
                },
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    disabledContainerColor = Color(0xFFBDBDBD)
                )
            ) {
                Text(
                    text = if (currentStep < 3) "Next" else "See Results",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (currentStep < 3) Icons.Default.ArrowForward else Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height((navigationBarHeight + 16).dp))
        }
    }
}

@Composable
private fun QuestionOne(selected: String, onSelect: (String) -> Unit) {
    QuestionCard(
        stepNumber = 1,
        question = "Do you see dark green streaks or line patterns on the leaf stem or midrib?",
        hint = "Look closely at the main vein of the leaf and the stem connecting it to the plant.",
        referenceImageRes = R.drawable.bbtv_streak_reference,
        referenceImageCaption = "Example: dot-dash dark green streaks on the midrib",
        options = listOf(
            OptionItem("yes", "Yes, I can see streaks or lines", Icons.Default.Visibility),
            OptionItem("no", "No, I don't see any streaks", Icons.Default.VisibilityOff),
            OptionItem("unsure", "Not sure / Hard to tell", Icons.Default.HelpOutline)
        ),
        selected = selected,
        onSelect = onSelect
    )
}

@Composable
private fun QuestionTwo(selected: String, onSelect: (String) -> Unit) {
    QuestionCard(
        stepNumber = 2,
        question = "How long have you noticed these symptoms, and how is the plant doing?",
        hint = "Think about when you first noticed the leaves looking different from normal.",
        options = listOf(
            OptionItem("new_stable", "Just noticed it (less than 1 week)", Icons.Default.FiberNew),
            OptionItem("weeks_stable", "1–2 weeks, staying about the same", Icons.Default.HorizontalRule),
            OptionItem("weeks_worse", "2–4 weeks and getting worse", Icons.Default.TrendingDown),
            OptionItem("month_worse", "More than a month and still worsening", Icons.Default.KeyboardDoubleArrowDown)
        ),
        selected = selected,
        onSelect = onSelect
    )
}

@Composable
private fun QuestionThree(selected: String, onSelect: (String) -> Unit) {
    QuestionCard(
        stepNumber = 3,
        question = "Are other plants nearby showing the same symptoms?",
        hint = "Check plants within about 5 meters (roughly 15 feet) of this one.",
        options = listOf(
            OptionItem("multiple", "Yes, several nearby plants are affected", Icons.Default.Groups),
            OptionItem("one_or_two", "Yes, 1–2 nearby plants look similar", Icons.Default.Group),
            OptionItem("only_this", "No, only this one plant", Icons.Default.Person),
            OptionItem("unsure", "Not sure / Haven't checked", Icons.Default.HelpOutline)
        ),
        selected = selected,
        onSelect = onSelect
    )
}

@Composable
private fun QuestionFour(selected: String, onSelect: (String) -> Unit) {
    QuestionCard(
        stepNumber = 4,
        question = "Check the base of the plant stem and where the leaves meet the stem. Do you see small dark brown or black insects clustered there?",
        hint = "Banana aphids are tiny, dark, and often found with ants nearby — ants protect them for their sticky secretion.",
        referenceImageRes = R.drawable.bbtv_aphid_reference,
        referenceImageCaption = "Example: banana aphids (Pentalonia nigronervosa) clustered at stem base",
        options = listOf(
            OptionItem("yes_insects", "Yes, I see small dark insects clustered there", Icons.Default.BugReport),
            OptionItem("ants_only", "I see ants but no small insects", Icons.Default.Warning),
            OptionItem("no_insects", "No insects or ants visible", Icons.Default.CheckCircle),
            OptionItem("not_checked", "I didn't check / Can't access that area", Icons.Default.HelpOutline)
        ),
        selected = selected,
        onSelect = onSelect
    )
}

data class OptionItem(val value: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun QuestionCard(
    stepNumber: Int,
    question: String,
    hint: String,
    options: List<OptionItem>,
    selected: String,
    onSelect: (String) -> Unit,
    referenceImageRes: Int? = null,
    referenceImageCaption: String = ""
) {
    var showImageViewer by remember { mutableStateOf(false) }

    if (showImageViewer && referenceImageRes != null) {
        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showImageViewer = false }
            ) {
                Image(
                    painter = painterResource(id = referenceImageRes),
                    contentDescription = referenceImageCaption,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showImageViewer = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    Column {
        // Step badge + question
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF2E7D32), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = question,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B5E20),
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = hint,
                    fontSize = 12.sp,
                    color = Color(0xFF757575),
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reference image — only shown when provided
        if (referenceImageRes != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showImageViewer = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.06f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REFERENCE IMAGE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Tap to enlarge",
                            fontSize = 10.sp,
                            color = Color(0xFF757575)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            painter = painterResource(id = referenceImageRes),
                            contentDescription = referenceImageCaption,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (referenceImageCaption.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(
                            text = referenceImageCaption,
                            fontSize = 11.sp,
                            color = Color(0xFF616161),
                            lineHeight = 15.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        options.forEach { option ->
            val isSelected = selected == option.value
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onSelect(option.value) }
                    .then(
                        if (isSelected) Modifier.border(2.dp, Color(0xFF2E7D32), RoundedCornerShape(12.dp))
                        else Modifier
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isSelected) Color(0xFF2E7D32) else Color(0xFFF5F5F5),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF757575),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = option.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF1B5E20) else Color(0xFF424242),
                        modifier = Modifier.weight(1f),
                        lineHeight = 20.sp
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun computeVerdict(streak: String, timeline: String, spread: String, aphid: String): BBTVQuestionnaireResult {
    var score = 0

    // Q1: Dot-dash streak — pathognomonic sign of BBTV, weighted heavily (max 3)
    // Seeing it clearly is near-diagnostic on its own
    when (streak) {
        "yes"    -> score += 3
        "unsure" -> score += 1
        // "no" -> 0, and acts as a cap (see below)
    }

    // Q2: Timeline — BBTV always progressively worsens, never stays stable (max 2)
    when (timeline) {
        "month_worse"  -> score += 2
        "weeks_worse"  -> score += 2
        "weeks_stable" -> score += 1
        // "new_stable" -> 0, too early to tell
    }

    // Q3: Spread to nearby plants — BBTV spreads in clusters via aphid movement (max 2)
    when (spread) {
        "multiple"   -> score += 2
        "one_or_two" -> score += 1
        // "only_this" -> 0, isolated cases are less typical
        // "unsure"    -> 0, no evidence either way
    }

    // Q4: Aphid presence — confirms vector but NOT the disease alone (max 1)
    // Deliberately capped at 1: aphids alone should never push to HIGH
    when (aphid) {
        "yes_insects" -> score += 1
        // "ants_only"  -> 0, ants without aphids is not meaningful
    }

    // Max score = 8
    // Gating rule: if streak is clearly absent ("no"), cap at MODERATE
    // because no other banana disease produces dot-dash streaks —
    // without it, BBTV is unlikely regardless of other signs
    val verdict = when {
        score >= 5 && streak == "yes" -> BBTVVerdict.HIGH
        score >= 3 || streak == "yes" -> BBTVVerdict.MODERATE
        else                          -> BBTVVerdict.LOW
    }

    return BBTVQuestionnaireResult(
        verdict = verdict,
        score = score,
        streakAnswer = streak,
        timelineAnswer = timeline,
        spreadAnswer = spread,
        aphidAnswer = aphid
    )
}
