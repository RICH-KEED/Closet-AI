package com.closetai.app.data.repository

import android.util.Log
import com.closetai.app.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

private const val TAG = "UserRepository"

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = firestore.collection("users")
    
    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val document = usersCollection.document(uid).get().await()
            Log.d(TAG, "getCurrentUser document exists: ${document.exists()}")
            document.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUser error", e)
            null
        }
    }
    
    suspend fun createUser(name: String, email: String): Result<User> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val user = User(
            uid = uid,
            name = name,
            email = email,
            createdAt = Timestamp.now()
        )
        return try {
            Log.d(TAG, "Creating user with uid: $uid")
            usersCollection.document(uid).set(user.toMap()).await()
            Log.d(TAG, "User created successfully")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "createUser error", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateOnboardingData(
        gender: String,
        genderOther: String,
        bodyType: String,
        clothingSize: String,
        height: String,
        weight: String,
        chestBust: String,
        waist: String,
        hip: String,
        inseam: String,
        skinTone: String,
        undertone: String,
        favoriteColors: List<String>,
        styles: List<String>,
        fitPreference: String,
        coveragePreference: String,
        occasions: List<String>,
        lifestyle: String,
        climate: String,
        budget: String,
        brandAttitude: String,
        sustainability: String,
        comfortFabric: List<String>,
        functionalFeatures: List<String>,
        culturalNeeds: List<String>,
        accessibilityNeeds: List<String>,
        clothingStyleTypes: List<String>,
        garmentTypes: List<String>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val currentUser = auth.currentUser
        
        Log.d(TAG, "updateOnboardingData starting for uid: $uid")
        
        return try {
            val data = mapOf(
                "uid" to uid,
                "email" to (currentUser?.email ?: ""),
                "name" to (currentUser?.displayName ?: ""),
                "gender" to gender,
                "genderOther" to genderOther,
                "bodyType" to bodyType,
                "clothingSize" to clothingSize,
                "height" to height,
                "weight" to weight,
                "chestBust" to chestBust,
                "waist" to waist,
                "hip" to hip,
                "inseam" to inseam,
                "skinTone" to skinTone,
                "undertone" to undertone,
                "favoriteColors" to favoriteColors,
                "styles" to styles,
                "fitPreference" to fitPreference,
                "coveragePreference" to coveragePreference,
                "occasions" to occasions,
                "lifestyle" to lifestyle,
                "climate" to climate,
                "budget" to budget,
                "brandAttitude" to brandAttitude,
                "sustainability" to sustainability,
                "comfortFabric" to comfortFabric,
                "functionalFeatures" to functionalFeatures,
                "culturalNeeds" to culturalNeeds,
                "accessibilityNeeds" to accessibilityNeeds,
                "clothingStyleTypes" to clothingStyleTypes,
                "garmentTypes" to garmentTypes,
                "onboardingCompleted" to true
            )
            
            // Use set with merge to create or update
            usersCollection.document(uid).set(data, SetOptions.merge()).await()
            Log.d(TAG, "updateOnboardingData success")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateOnboardingData error", e)
            Result.failure(e)
        }
    }
    
    suspend fun isOnboardingCompleted(): Boolean {
        val user = getCurrentUser()
        return user?.onboardingCompleted == true
    }
    
    fun signOut() {
        auth.signOut()
    }
}
