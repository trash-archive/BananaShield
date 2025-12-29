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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
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

        // ✅ ONE-TIME MIGRATION: Uncomment this line, run app once, then comment again
        // migrateNotifications()

        setContent {
            BananaShieldTheme {
                var showSplash by remember { mutableStateOf(true) }
                val navController = rememberNavController()

                // Check if user is already logged in
                val currentUser = auth.currentUser
                val startDestination = if (currentUser != null) "home" else "login"

                // ✅ Deep link state - persists across intents
                var deepLinkTab by remember { mutableStateOf<Int?>(null) }
                var deepLinkScanId by remember { mutableStateOf<String?>(null) }

                // ✅ Handle INITIAL intent (app launch/cold start)
                LaunchedEffect(Unit) {
                    handleDeepLink(intent)?.let { (tab, scanId) ->
                        deepLinkTab = tab
                        deepLinkScanId = scanId
                        android.util.Log.d("MainActivity", "✅ Initial deep link: tab=$tab, scanId=$scanId")
                    }
                }

                if (showSplash) {
                    SplashScreen(
                        onTimeout = { showSplash = false }
                    )
                } else {
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
                                        android.util.Log.d("MainActivity", "✅ Deep link handled - state cleared")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // ✅ Handle notification when app is already open
        handleDeepLink(intent)?.let { (tab, scanId) ->
            android.util.Log.d("MainActivity", "✅ New intent (app open): tab=$tab, scanId=$scanId")

            // ✅ Recreate activity to trigger HomePage with new deep link
            recreate()
        }
    }

    // ✅ Deep link handler
    private fun handleDeepLink(intent: Intent?): Pair<Int, String?>? {
        val destination = intent?.getStringExtra("destination")
        val scanId = intent?.getStringExtra("scanId")

        android.util.Log.d("MainActivity", "Handling deep link: destination=$destination, scanId=$scanId")

        return if (destination == "scan_result" && scanId != null) {
            android.util.Log.d("MainActivity", "✅ Valid deep link - Navigate to History with scan: $scanId")
            Pair(3, scanId) // Tab 3 is History
        } else {
            null
        }
    }

    // ✅ ONE-TIME MIGRATION: Rename isRead → read in existing Firestore notifications
    private fun migrateNotifications() {
        val db = Firebase.firestore

        android.util.Log.d("Migration", "🔄 Starting notification migration...")

        db.collection("notifications")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    android.util.Log.d("Migration", "⚠️ No notifications to migrate")
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                var migratedCount = 0

                snapshot.documents.forEach { doc ->
                    // Check if old field exists
                    if (doc.contains("isRead")) {
                        val isReadValue = doc.getBoolean("isRead") ?: false

                        // Update: add "read" field and remove "isRead" field
                        batch.update(doc.reference, mapOf(
                            "read" to isReadValue,
                            "isRead" to FieldValue.delete()
                        ))

                        migratedCount++
                    }
                }

                if (migratedCount > 0) {
                    batch.commit()
                        .addOnSuccessListener {
                            android.util.Log.d("Migration", "✅ Successfully migrated $migratedCount notifications (isRead → read)")
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("Migration", "❌ Migration failed: ${e.message}", e)
                        }
                } else {
                    android.util.Log.d("Migration", "✅ All notifications already migrated")
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Migration", "❌ Failed to fetch notifications for migration: ${e.message}", e)
            }
    }
}
