package com.example.bananashield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationCodeScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var timeLeft by remember { mutableStateOf(300) } // 5 minutes in seconds
    val scope = rememberCoroutineScope()

    // Countdown timer
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2E7D32),
                        Color(0xFF1B5E20)
                    )
                )
            )
    ) {
        // Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Check Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF81C784)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD54F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Verification",
                        tint = Color(0xFF1B5E20),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Enter Verification Code",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle with email
            Text(
                text = "We've sent a 6-digit code to",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = email,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFD54F)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Code Input Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                repeat(6) { index ->
                    CodeDigitBox(
                        digit = code.getOrNull(index)?.toString() ?: "",
                        isFocused = code.length == index
                    )
                }
            }

            // Hidden TextField for input
            BasicTextField(
                value = code,
                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) code = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.size(0.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Timer
            Text(
                text = "Code expires in",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = String.format("%d:%02d", minutes, seconds),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Resend Code
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn't receive the code?",
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = {
                    // TODO: Resend code
                    timeLeft = 300
                }) {
                    Text(
                        text = "Resend Code",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Verify Button
            Button(
                onClick = {
                    if (code.length == 6) {
                        // TODO: Verify code with backend
                        // For now, accept any 6-digit code
                        onNavigateToChangePassword()
                    } else {
                        errorMessage = "Please enter the complete 6-digit code"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD54F)
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = code.length == 6
            ) {
                Text(
                    text = "Verify",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Help text
            Text(
                text = "Check your spam folder if you don't see\nthe email in your inbox",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CodeDigitBox(digit: String, isFocused: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (digit.isNotEmpty()) Color(0xFF66BB6A) else Color(0xFF4CAF50).copy(alpha = 0.3f)
            )
            .border(
                width = 2.dp,
                color = if (isFocused) Color(0xFFFFD54F) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
