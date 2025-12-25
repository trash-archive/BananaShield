package com.example.bananashield

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FirestoreHelper {
    private val db: FirebaseFirestore = Firebase.firestore
    private const val TAG = "FirestoreHelper"

    init {
        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true)
        Log.d(TAG, "FirestoreHelper initialized")
    }

    // Check if user exists and save only if new
    fun checkAndSaveUserData(
        userId: String,
        firstName: String,
        lastName: String,
        email: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Checking if user exists: $userId")

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(TAG, "✓ User already exists, skipping save")
                    onSuccess()
                } else {
                    Log.d(TAG, "✗ User doesn't exist, creating new profile")
                    // User doesn't exist, create new profile
                    saveUserData(
                        userId = userId,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = "",
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ Failed to check user existence", exception)
                onFailure(exception)
            }
    }

    // Save user data after registration
    fun saveUserData(
        userId: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Attempting to save user data for userId: $userId")
        Log.d(TAG, "Data: firstName=$firstName, lastName=$lastName, email=$email")

        val user = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "phone" to phone,
            "location" to "Cebu City, Philippines",
            "farmSize" to "2.5",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "✓ User data saved successfully to Firestore!")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ Failed to save user data to Firestore", exception)
                onFailure(exception)
            }
    }

    // Get user data
    fun getUserData(
        userId: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Fetching user data for userId: $userId")

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(TAG, "✓ User data retrieved successfully")
                    onSuccess(document.data ?: emptyMap())
                } else {
                    Log.w(TAG, "No user data found")
                    onFailure(Exception("User not found"))
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ Failed to get user data", exception)
                onFailure(exception)
            }
    }

    // Update user data
    fun updateUserData(
        userId: String,
        firstName: String,
        lastName: String,
        phone: String,
        location: String,
        farmSize: String,
        profileImageUrl: String? = null,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Updating user data for userId: $userId")
        Log.d(TAG, "Data: firstName=$firstName, lastName=$lastName, phone=$phone")

        val updates = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "phone" to phone,
            "location" to location,
            "farmSize" to farmSize,
            "updatedAt" to System.currentTimeMillis()
        )

        // Add profile image URL if provided
        profileImageUrl?.let {
            updates["profileImageUrl"] = it
            Log.d(TAG, "Including profile image URL in update")
        }

        db.collection("users")
            .document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "✓ User data updated successfully!")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ Failed to update user data", exception)
                onFailure(exception)
            }
    }

    // Check if user exists and determine sign-in method
    fun checkUserSignInMethod(
        email: String,
        onSuccess: (Boolean) -> Unit, // Returns true if Google user, false if email/password user
        onNotFound: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Checking sign-in method for email: $email")

        db.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "✗ No user found with email: $email")
                    onNotFound()
                } else {
                    Log.d(TAG, "✓ User found with email: $email")
                    val userId = documents.documents[0].id

                    // Check Firebase Auth to determine sign-in method
                    Firebase.auth.fetchSignInMethodsForEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val signInMethods = task.result?.signInMethods ?: emptyList()
                                val isGoogleUser = signInMethods.contains("google.com")
                                Log.d(TAG, "Sign-in methods: $signInMethods, isGoogleUser: $isGoogleUser")
                                onSuccess(isGoogleUser)
                            } else {
                                // If fetchSignInMethodsForEmail fails, assume email/password
                                Log.d(TAG, "Failed to fetch sign-in methods, assuming email/password user")
                                onSuccess(false)
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ Failed to check user sign-in method", exception)
                onFailure(exception)
            }
    }

    // Save scan result
    fun saveScanResult(
        userId: String,
        result: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Attempting to save scan result for userId: $userId")

        val scan = hashMapOf(
            "userId" to userId,
            "result" to result,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("scans")
            .add(scan)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "✓ Scan saved successfully with ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ Failed to save scan", exception)
                onFailure(exception)
            }
    }

    // Get all scans for a user
    fun getUserScans(
        userId: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Fetching scans for userId: $userId")

        db.collection("scans")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val scans = documents.map { it.data }
                Log.d(TAG, "✓ Retrieved ${scans.size} scans")
                onSuccess(scans)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ Failed to get scans", exception)
                onFailure(exception)
            }
    }
}
