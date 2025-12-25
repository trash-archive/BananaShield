package com.example.bananashield

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onNavigateToLogin: () -> Unit = {},
    onRegistrationSuccess: () -> Unit = {}
) {
    val auth = Firebase.auth
    val context = LocalContext.current

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    // Configure Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    isLoading = true
                    Log.d("Registration", "Google Sign-In token received")
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                Log.d("Registration", "Google Auth successful")
                                val user = auth.currentUser
                                user?.let {
                                    val nameParts = it.displayName?.split(" ") ?: listOf("", "")
                                    FirestoreHelper.saveUserData(
                                        userId = it.uid,
                                        firstName = nameParts.getOrNull(0) ?: "",
                                        lastName = nameParts.getOrNull(1) ?: "",
                                        email = it.email ?: "",
                                        phone = "",
                                        onSuccess = {
                                            isLoading = false
                                            onRegistrationSuccess()
                                        },
                                        onFailure = { exception ->
                                            isLoading = false
                                            errorMessage = "Registration successful but failed to save data: ${exception.message}"
                                            onRegistrationSuccess()
                                        }
                                    )
                                }
                            } else {
                                isLoading = false
                                errorMessage = authTask.exception?.message ?: "Authentication failed"
                            }
                        }
                }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Google sign-in failed: ${e.message}"
                Log.e("Registration", "Google sign-in failed", e)
            }
        }
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "BananaShield Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = "Create Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Join BananaShield today",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // First Name Field
            CustomTextField(
                value = firstName,
                onValueChange = { firstName = it },
                placeholder = "First Name",
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Last Name Field
            CustomTextField(
                value = lastName,
                onValueChange = { lastName = it },
                placeholder = "Last Name",
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email Field
            CustomTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                icon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Field
            CustomTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "Phone Number",
                icon = Icons.Default.Phone
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field with Toggle
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        text = "Password",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Lock else Icons.Default.Lock,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Terms Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFFD54F),
                        uncheckedColor = Color.White,
                        checkmarkColor = Color(0xFF1B5E20)
                    )
                )
                Text(
                    text = "I agree to the Terms & Conditions",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            // Error Message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF5252).copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sign Up Button
            Button(
                onClick = {
                    errorMessage = null

                    if (!agreedToTerms) {
                        errorMessage = "Please agree to Terms & Conditions"
                        return@Button
                    }
                    if (firstName.isBlank() || lastName.isBlank()) {
                        errorMessage = "Please enter your full name"
                        return@Button
                    }
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email and password are required"
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                        return@Button
                    }

                    isLoading = true
                    Log.d("Registration", "Starting registration for email: $email")

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("Registration", "Firebase Auth successful")
                                val user = auth.currentUser

                                if (user != null) {
                                    val profileUpdates = userProfileChangeRequest {
                                        displayName = "$firstName $lastName"
                                    }

                                    user.updateProfile(profileUpdates)
                                        .addOnCompleteListener { profileTask ->
                                            if (profileTask.isSuccessful) {
                                                Log.d("Registration", "Profile updated, saving to Firestore")

                                                // Save to Firestore
                                                FirestoreHelper.saveUserData(
                                                    userId = user.uid,
                                                    firstName = firstName,
                                                    lastName = lastName,
                                                    email = email,
                                                    phone = phone,
                                                    onSuccess = {
                                                        Log.d("Registration", "Firestore save successful!")
                                                        isLoading = false
                                                        onRegistrationSuccess()
                                                    },
                                                    onFailure = { exception ->
                                                        Log.e("Registration", "Firestore save failed", exception)
                                                        isLoading = false
                                                        errorMessage = "Registration successful but failed to save data: ${exception.message}"
                                                        onRegistrationSuccess()
                                                    }
                                                )
                                            } else {
                                                Log.e("Registration", "Profile update failed")
                                                isLoading = false
                                                errorMessage = "Failed to update profile: ${profileTask.exception?.message}"
                                            }
                                        }
                                } else {
                                    Log.e("Registration", "User is null after registration")
                                    isLoading = false
                                    errorMessage = "Registration failed: User not found"
                                }
                            } else {
                                Log.e("Registration", "Firebase Auth failed", task.exception)
                                isLoading = false
                                errorMessage = task.exception?.message ?: "Registration failed"
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD54F)
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF1B5E20),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign Up",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OR Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = "  OR  ",
                    color = Color.White,
                    fontSize = 14.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In Button
            OutlinedButton(
                onClick = {
                    errorMessage = null
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !isLoading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Google",
                        tint = Color(0xFF1B5E20),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign up with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1B5E20)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Already have account
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    color = Color.White,
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Log In",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.6f)
            )
        },
        trailingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f)
            )
        },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
            cursorColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
