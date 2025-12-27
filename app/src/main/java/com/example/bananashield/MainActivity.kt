package com.example.bananashield

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bananashield.ui.theme.BananaShieldTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    // ✅ Notification permission launcher (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ Notification permission granted")
        } else {
            android.util.Log.d("MainActivity", "❌ Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ✅ Request notification permission (Android 13+ only)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Initialize Firebase Auth
        auth = Firebase.auth

        setContent {
            BananaShieldTheme {
                val navController = rememberNavController()

                // Check if user is already logged in
                val currentUser = auth.currentUser
                val startDestination = if (currentUser != null) "home" else "login"

                // ✅ FIXED: Mutable state for deep linking - persists across intents
                var deepLinkTab by remember { mutableStateOf<Int?>(null) }
                var deepLinkScanId by remember { mutableStateOf<String?>(null) }

                // Handle INITIAL intent (app launch/cold start)
                LaunchedEffect(Unit) {
                    handleNotificationIntent(intent) { tab, scanId ->
                        deepLinkTab = tab
                        deepLinkScanId = scanId
                        android.util.Log.d("MainActivity", "✅ Initial intent: tab=$tab, scanId=$scanId")
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0.dp)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                onNavigateToRegistration = {
                                    navController.navigate("registration")
                                },
                                onNavigateToForgotPassword = {
                                    navController.navigate("forgotPassword")
                                },
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("registration") {
                            RegistrationScreen(
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                },
                                onRegistrationSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("registration") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("forgotPassword") {
                            ForgotPasswordScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onEmailSent = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("home") {
                            HomePage(
                                initialTab = deepLinkTab,
                                deepLinkScanId = deepLinkScanId,
                                onDeepLinkHandled = {
                                    deepLinkTab = null
                                    deepLinkScanId = null
                                    android.util.Log.d("MainActivity", "✅ Deep link handled - cleared state")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // ✅ FIXED: Handle notification when app is already open
        handleNotificationIntent(intent) { tab, scanId ->
            android.util.Log.d("MainActivity", "✅ New intent (app open): tab=$tab, scanId=$scanId")
            // Update the Compose state directly - triggers recomposition!
            // This will update HomePage → HistoryContent → HistoryDetailScreen
        }
    }

    private fun handleNotificationIntent(
        intent: Intent?,
        onNavigate: (tab: Int, scanId: String?) -> Unit
    ) {
        intent?.let {
            val destination = it.getStringExtra("destination")
            val scanId = it.getStringExtra("scanId")

            android.util.Log.d("MainActivity", "Handling intent: destination=$destination, scanId=$scanId")

            if (destination == "scan_result" && scanId != null) {
                // Navigate to History tab (index 3) with scan ID
                onNavigate(3, scanId)
            }
        }
    }
}
