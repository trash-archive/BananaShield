package com.example.bananashield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bananashield.ui.theme.BananaShieldTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase Auth
        auth = Firebase.auth

        setContent {
            BananaShieldTheme {
                val navController = rememberNavController()

                // Check if user is already logged in
                val currentUser = auth.currentUser
                val startDestination = if (currentUser != null) "home" else "login"

                var isInitialCheck by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    isInitialCheck = false
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                            HomePage()
                        }
                    }
                }
            }
        }
    }
}
