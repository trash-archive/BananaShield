package com.example.bananashield

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object ContactHelper {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private const val COLLECTION_CONTACTS = "contact_messages"
    private const val TAG = "ContactHelper"

    fun sendContactMessage(
        userName: String,
        subject: String,
        message: String,
        onSuccess: () -> Unit,
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

        Log.d(TAG, "📤 Sending contact message from: $userName ($userEmail)")

        val contactData = hashMapOf(
            // User Information
            "userId" to userId,
            "userName" to userName,
            "userEmail" to userEmail,

            // Message Content
            "subject" to subject,
            "message" to message,

            // Metadata
            "timestamp" to System.currentTimeMillis(),
            "status" to "unread",
            "deviceInfo" to android.os.Build.MODEL,
            "appVersion" to "1.0.0",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection(COLLECTION_CONTACTS)
            .add(contactData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "✅ Message sent successfully! ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to send message: ${e.message}")
                e.printStackTrace()
                onFailure(e)
            }
    }

    // For admin to fetch all contact messages
    fun getAllContactMessages(
        onSuccess: (List<ContactMessage>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "📥 Fetching all contact messages...")

        db.collection(COLLECTION_CONTACTS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "✅ Found ${documents.size()} contact messages")
                val messageList = documents.mapNotNull { doc ->
                    try {
                        ContactMessage(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            userEmail = doc.getString("userEmail") ?: "",
                            subject = doc.getString("subject") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            status = doc.getString("status") ?: "unread",
                            deviceInfo = doc.getString("deviceInfo") ?: "",
                            appVersion = doc.getString("appVersion") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing document: ${e.message}")
                        null
                    }
                }
                onSuccess(messageList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to fetch messages: ${e.message}")
                onFailure(e)
            }
    }

    // Mark message as read (for admin)
    fun markAsRead(
        messageId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_CONTACTS)
            .document(messageId)
            .update("status", "read")
            .addOnSuccessListener {
                Log.d(TAG, "✅ Message marked as read")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to mark as read: ${e.message}")
                onFailure(e)
            }
    }

    // Delete contact message (for admin)
    fun deleteContactMessage(
        messageId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_CONTACTS)
            .document(messageId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Message deleted")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to delete message: ${e.message}")
                onFailure(e)
            }
    }
}
