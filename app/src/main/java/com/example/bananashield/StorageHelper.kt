package com.example.bananashield

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase
import java.util.UUID

object StorageHelper {
    private val storage: FirebaseStorage = Firebase.storage
    private const val TAG = "StorageHelper"

    // Upload profile image
    fun uploadProfileImage(
        userId: String,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Uploading profile image for userId: $userId")

        val fileName = "profile_$userId.jpg"
        val storageRef = storage.reference.child("profile_images/$fileName")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                Log.d(TAG, "✓ Profile image uploaded successfully")
                // Get download URL
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    Log.d(TAG, "✓ Download URL: ${uri.toString()}")
                    onSuccess(uri.toString())
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "✗ Failed to get download URL", exception)
                    onFailure(exception)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ Failed to upload profile image", exception)
                onFailure(exception)
            }
    }

    // Upload scan image
    fun uploadImage(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onProgress: (Double) -> Unit
    ) {
        Log.d(TAG, "Uploading scan image")

        val fileName = "scan_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("scans/$fileName")

        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            Log.d(TAG, "Upload progress: $progress%")
            onProgress(progress)
        }.addOnSuccessListener {
            Log.d(TAG, "✓ Scan image uploaded successfully")
            // Get download URL
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Log.d(TAG, "✓ Download URL: ${uri.toString()}")
                onSuccess(uri.toString())
            }.addOnFailureListener { exception ->
                Log.e(TAG, "✗ Failed to get download URL", exception)
                onFailure(exception)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "✗ Failed to upload scan image", exception)
            onFailure(exception)
        }
    }
}
