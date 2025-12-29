package com.example.bananashield

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Enhanced data models for complete storage
data class ScanHistory(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",

    // Disease Information
    val diseaseName: String = "",
    val scientificName: String = "",
    val diseaseType: String = "",
    val confidence: Float = 0f,
    val confidenceLevel: String = "",
    val severity: String = "",

    // Symptoms & Causes
    val symptoms: List<String> = emptyList(),
    val causes: List<String> = emptyList(),

    // Treatment Information
    val treatmentSteps: List<Map<String, String>> = emptyList(),
    val safetyNotes: List<String> = emptyList(),

    // Prevention Information
    val preventiveMeasures: List<Map<String, Any>> = emptyList(),

    // Metadata
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String = "",
    val location: String = "",
    val notes: String = ""
)

object ScanHistoryHelper {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private const val COLLECTION_SCANS = "scan_history"
    private const val TAG = "ScanHistoryHelper"

    // ✅ ADDED: Track which scans already have notifications to prevent duplicates
    private val notifiedScans = mutableSetOf<String>()

    fun saveScanResult(
        bitmap: Bitmap,
        classification: Classification,
        location: String = "",
        notes: String = "",
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e(TAG, "❌ User not authenticated!")
            onFailure(Exception("User not authenticated"))
            return
        }

        val userId = currentUser.uid
        val userEmail = currentUser.email ?: "No email"
        val userName = currentUser.displayName ?: "Anonymous User"

        Log.d(TAG, "✅ Starting save for user: $userId ($userEmail)")

        // Compress image
        val compressedBytes = try {
            compressBitmap(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error compressing bitmap: ${e.message}")
            onFailure(e)
            return
        }

        // Upload to Firebase Storage
        val timestamp = System.currentTimeMillis()
        val imageFileName = "scans/$userId/${timestamp}_scan.jpg"
        val storageRef = storage.reference.child(imageFileName)

        Log.d(TAG, "📤 Uploading image to Storage...")

        storageRef.putBytes(compressedBytes)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Image uploaded successfully")

                // Get download URL
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    Log.d(TAG, "✅ Got download URL: $imageUrl")

                    // Convert treatment steps to Map format for Firestore
                    val treatmentStepsMaps = classification.diseaseInfo.treatmentSteps.map { step ->
                        mapOf(
                            "title" to step.title,
                            "description" to step.description,
                            "icon" to step.icon
                        )
                    }

                    // Convert preventive measures to Map format for Firestore
                    val preventiveMeasuresMaps = classification.diseaseInfo.preventiveMeasures.map { measure ->
                        mapOf(
                            "category" to measure.category,
                            "title" to measure.title,
                            "steps" to measure.steps,
                            "icon" to measure.icon
                        )
                    }

                    // Create comprehensive scan data
                    val scanData = hashMapOf(
                        // User Information
                        "userId" to userId,
                        "userName" to userName,
                        "userEmail" to userEmail,

                        // Disease Information
                        "diseaseName" to classification.diseaseInfo.name,
                        "scientificName" to classification.diseaseInfo.scientificName,
                        "diseaseType" to classification.diseaseInfo.diseaseType,
                        "confidence" to classification.confidence,
                        "confidenceLevel" to classification.diseaseInfo.confidenceLevel,
                        "severity" to classification.diseaseInfo.severity,

                        // Symptoms & Causes
                        "symptoms" to classification.diseaseInfo.symptoms,
                        "causes" to classification.diseaseInfo.causes,

                        // Treatment Information
                        "treatmentSteps" to treatmentStepsMaps,
                        "safetyNotes" to classification.diseaseInfo.safetyNotes,

                        // Prevention Information
                        "preventiveMeasures" to preventiveMeasuresMaps,

                        // Metadata
                        "timestamp" to timestamp,
                        "imageUrl" to imageUrl,
                        "location" to location,
                        "notes" to notes,

                        // Additional metadata for admin dashboard
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "deviceInfo" to android.os.Build.MODEL,
                        "appVersion" to "1.0.0"
                    )

                    Log.d(TAG, "📤 Saving complete data to Firestore...")

                    db.collection(COLLECTION_SCANS)
                        .add(scanData)
                        .addOnSuccessListener { documentReference ->
                            val scanId = documentReference.id
                            Log.d(TAG, "✅ Scan saved with ID: $scanId")

                            // ✅ FIXED: Only notify once using a tracking set (no delay needed)
                            if (!notifiedScans.contains(scanId)) {
                                notifiedScans.add(scanId)

                                Log.d(TAG, "🔔 Creating notification for scan: $scanId")
                                NotificationHelper.notifyScanComplete(
                                    userId = currentUser.uid,
                                    scanId = scanId,
                                    diseaseName = classification.diseaseInfo.name,
                                    confidence = classification.confidence
                                )
                            } else {
                                Log.d(TAG, "⚠️ Notification already created for scanId: $scanId")
                            }

                            onSuccess(scanId)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Failed to save to Firestore: ${e.message}")
                            e.printStackTrace()
                            onFailure(e)
                        }

                }.addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to get download URL: ${e.message}")
                    onFailure(e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to upload image: ${e.message}")
                e.printStackTrace()
                onFailure(e)
            }
    }

    fun getUserScanHistory(
        onSuccess: (List<ScanHistory>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            Log.e(TAG, "❌ Cannot fetch history: User not authenticated!")
            onFailure(Exception("User not authenticated"))
            return
        }

        Log.d(TAG, "📥 Fetching scan history for user: $userId")

        db.collection(COLLECTION_SCANS)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "✅ Found ${documents.size()} scans")
                val scanList = documents.mapNotNull { doc ->
                    try {
                        ScanHistory(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            userEmail = doc.getString("userEmail") ?: "",
                            diseaseName = doc.getString("diseaseName") ?: "",
                            scientificName = doc.getString("scientificName") ?: "",
                            diseaseType = doc.getString("diseaseType") ?: "",
                            confidence = doc.getDouble("confidence")?.toFloat() ?: 0f,
                            confidenceLevel = doc.getString("confidenceLevel") ?: "",
                            severity = doc.getString("severity") ?: "",
                            symptoms = doc.get("symptoms") as? List<String> ?: emptyList(),
                            causes = doc.get("causes") as? List<String> ?: emptyList(),
                            treatmentSteps = doc.get("treatmentSteps") as? List<Map<String, String>> ?: emptyList(),
                            safetyNotes = doc.get("safetyNotes") as? List<String> ?: emptyList(),
                            preventiveMeasures = doc.get("preventiveMeasures") as? List<Map<String, Any>> ?: emptyList(),
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            location = doc.getString("location") ?: "",
                            notes = doc.getString("notes") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing document: ${e.message}")
                        null
                    }
                }
                onSuccess(scanList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to fetch history: ${e.message}")
                onFailure(e)
            }
    }

    // ✅ Get single scan history by ID (for deep linking from notifications)
    fun getScanHistoryById(
        scanId: String,
        onSuccess: (ScanHistory) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "📥 Fetching scan with ID: $scanId")

        db.collection(COLLECTION_SCANS)
            .document(scanId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        val scanHistory = ScanHistory(
                            id = document.id,
                            userId = document.getString("userId") ?: "",
                            userName = document.getString("userName") ?: "",
                            userEmail = document.getString("userEmail") ?: "",
                            diseaseName = document.getString("diseaseName") ?: "",
                            scientificName = document.getString("scientificName") ?: "",
                            diseaseType = document.getString("diseaseType") ?: "",
                            confidence = document.getDouble("confidence")?.toFloat() ?: 0f,
                            confidenceLevel = document.getString("confidenceLevel") ?: "",
                            severity = document.getString("severity") ?: "",
                            symptoms = document.get("symptoms") as? List<String> ?: emptyList(),
                            causes = document.get("causes") as? List<String> ?: emptyList(),
                            treatmentSteps = document.get("treatmentSteps") as? List<Map<String, String>> ?: emptyList(),
                            safetyNotes = document.get("safetyNotes") as? List<String> ?: emptyList(),
                            preventiveMeasures = document.get("preventiveMeasures") as? List<Map<String, Any>> ?: emptyList(),
                            timestamp = document.getLong("timestamp") ?: 0L,
                            imageUrl = document.getString("imageUrl") ?: "",
                            location = document.getString("location") ?: "",
                            notes = document.getString("notes") ?: ""
                        )

                        Log.d(TAG, "✅ Successfully fetched scan: ${scanHistory.diseaseName}")
                        onSuccess(scanHistory)

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing scan document: ${e.message}")
                        onFailure(e)
                    }
                } else {
                    Log.e(TAG, "❌ Scan document not found: $scanId")
                    onFailure(Exception("Scan not found"))
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Failed to fetch scan: ${exception.message}")
                onFailure(exception)
            }
    }

    // Delete multiple scans (batch delete)
    fun deleteScans(
        scanIds: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (scanIds.isEmpty()) {
            onSuccess()
            return
        }

        Log.d(TAG, "🗑️ Deleting ${scanIds.size} scans...")

        val batch = db.batch()

        // Add all delete operations to batch
        scanIds.forEach { scanId ->
            val docRef = db.collection(COLLECTION_SCANS).document(scanId)
            batch.delete(docRef)
        }

        // Commit the batch delete
        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Successfully deleted ${scanIds.size} scans")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to delete scans: ${e.message}")
                onFailure(e)
            }
    }

    // Delete single scan
    fun deleteScan(
        scanId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        deleteScans(listOf(scanId), onSuccess, onFailure)
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
        val bytes = outputStream.toByteArray()
        Log.d(TAG, "Compressed image size: ${bytes.size / 1024} KB")
        return bytes
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
