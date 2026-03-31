package com.closetai.app.data.repository

import android.net.Uri
import android.util.Log
import com.closetai.app.data.model.WardrobeItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import java.util.UUID

class WardrobeRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val tag = "WardrobeRepository"

    suspend fun uploadWardrobeItem(imageUri: Uri, category: String, color: String): Result<WardrobeItem> {
        val user = auth.currentUser
        if (user == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        
        val uid = user.uid
        val itemId = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("users/$uid/wardrobe/$itemId.jpg")

        return try {
            // Upload the image to Firebase Storage
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Save the metadata to Firestore
            val wardrobeItem = WardrobeItem(
                id = itemId,
                userId = uid,
                imageUrl = downloadUrl,
                category = category,
                color = color,
                addedAt = System.currentTimeMillis()
            )

            firestore.collection("users").document(uid).collection("wardrobe")
                .document(itemId).set(wardrobeItem).await()

            Result.success(wardrobeItem)
        } catch (e: Exception) {
            Result.failure(mapWardrobeException(e))
        }
    }

    suspend fun fetchWardrobeItems(): Result<List<WardrobeItem>> {
        val user = auth.currentUser
        if (user == null) {
            return Result.failure(Exception("User not authenticated"))
        }

        val uid = user.uid

        return try {
            val snapshot = firestore.collection("users").document(uid).collection("wardrobe")
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .get().await()

            // Map defensively so older/variant field names (e.g., imageUrl vs image_url) still work.
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                val id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id
                val userId = (data["userId"] as? String)?.takeIf { it.isNotBlank() } ?: (data["uid"] as? String).orEmpty()
                val category = (data["category"] as? String).orEmpty()
                val color = (data["color"] as? String).orEmpty()

                // Support both naming conventions.
                val imageUrl = (data["imageUrl"] as? String)
                    ?.takeIf { it.isNotBlank() }
                    ?: (data["image_url"] as? String).orEmpty()

                val addedAtAny = data["addedAt"]
                val addedAt = when (addedAtAny) {
                    is com.google.firebase.Timestamp -> addedAtAny.toDate().time
                    is Number -> addedAtAny.toLong()
                    else -> System.currentTimeMillis()
                }

                WardrobeItem(
                    id = id,
                    userId = userId,
                    imageUrl = imageUrl,
                    category = category,
                    color = color,
                    addedAt = max(addedAt, 0L)
                )
            }

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(mapWardrobeException(e))
        }
    }

    private fun mapWardrobeException(error: Exception): Exception {
        return when (error) {
            is FirebaseFirestoreException -> {
                Log.e(tag, "Firestore error code=${error.code}", error)
                when (error.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        Exception(
                            "Missing or insufficient Firestore permissions. " +
                                "Deploy firestore.rules and ensure user is signed in."
                        )
                    }
                    else -> Exception(error.localizedMessage ?: "Firestore request failed")
                }
            }
            is StorageException -> {
                Log.e(tag, "Storage error code=${error.errorCode}", error)
                when (error.errorCode) {
                    StorageException.ERROR_NOT_AUTHENTICATED -> {
                        Exception("User not authenticated for storage upload.")
                    }
                    StorageException.ERROR_NOT_AUTHORIZED -> {
                        Exception(
                            "Missing or insufficient Storage permissions. " +
                                "Deploy storage.rules and ensure user is signed in."
                        )
                    }
                    else -> Exception(error.localizedMessage ?: "Storage request failed")
                }
            }
            else -> {
                Log.e(tag, "Unknown wardrobe error", error)
                Exception(error.localizedMessage ?: "Wardrobe operation failed")
            }
        }
    }
}
