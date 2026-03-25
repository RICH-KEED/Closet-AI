package com.closetai.app.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.closetai.app.R
import com.closetai.app.data.repository.UserRepository
import com.closetai.app.ui.components.GlassmorphicCard
import com.closetai.app.ui.theme.Primary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "SignInScreen"

@Composable
fun SignInScreen(
    onSignInSuccess: (needsOnboarding: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Theme-aware colors
    val isDarkTheme = isSystemInDarkTheme()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        
        // Decorative blob
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-50).dp)
                .size(200.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "ClosetAI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Headline
            Text(
                text = "Welcome to your",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "digital closet",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Glassmorphic card
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Primary.copy(alpha = 0.1f),
                                RoundedCornerShape(50)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Checkroom,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Sign in to curate your style, organize your wardrobe, and get AI-powered outfit suggestions.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Google Sign-In Button
                    GoogleSignInButton(
                        isLoading = isLoading,
                        isDarkTheme = isDarkTheme,
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null

                                try {
                                    Log.d(TAG, "Starting Google Sign-In...")
                                    val result = signInWithGoogle(context)
                                    result.onSuccess { needsOnboarding ->
                                        Log.d(TAG, "Sign-in successful, needsOnboarding: $needsOnboarding")
                                        onSignInSuccess(needsOnboarding)
                                    }.onFailure { e ->
                                        Log.e(TAG, "Sign-in failed", e)
                                        errorMessage = e.message ?: "Sign in failed"
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Sign-in exception", e)
                                    errorMessage = e.message ?: "Sign in failed"
                                    isLoading = false
                                }
                            }
                        }
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Footer
        Text(
            text = "By continuing, you agree to our Terms of Service & Privacy Policy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 32.dp, end = 32.dp)
        )
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    // Theme-aware colors
    val buttonBackground = if (isDarkTheme) {
        MaterialTheme.colorScheme.surface
    } else {
        Color.White
    }
    
    val textColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFF1F1F1F)
    }
    
    val borderColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    } else {
        Color(0xFFDADCE0)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(buttonBackground)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Primary
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google "G" logo
                Text(
                    text = "G",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

private suspend fun signInWithGoogle(context: Context): Result<Boolean> {
    val credentialManager = CredentialManager.create(context)
    val auth = FirebaseAuth.getInstance()
    val userRepository = UserRepository()
    
    // Web client ID from google-services.json
    val webClientId = context.getString(R.string.default_web_client_id)
    Log.d(TAG, "Using web client ID: $webClientId")
    
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .build()
    
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    
    return try {
        val result = credentialManager.getCredential(context, request)
        handleSignInResult(result, auth, userRepository)
    } catch (e: Exception) {
        Log.e(TAG, "getCredential failed", e)
        Result.failure(e)
    }
}

private suspend fun handleSignInResult(
    result: GetCredentialResponse,
    auth: FirebaseAuth,
    userRepository: UserRepository
): Result<Boolean> {
    val credential = result.credential
    Log.d(TAG, "Credential type: ${credential.type}")
    
    if (credential is CustomCredential && 
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            Log.d(TAG, "Got Google ID token")
            
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            Log.d(TAG, "Firebase auth successful")
            
            val user = authResult.user
            if (user != null) {
                Log.d(TAG, "User UID: ${user.uid}")
                
                // Check if user exists in Firestore
                try {
                    val existingUser = userRepository.getCurrentUser()
                    Log.d(TAG, "Existing user: $existingUser")
                    
                    if (existingUser == null) {
                        // Create new user
                        Log.d(TAG, "Creating new user...")
                        userRepository.createUser(
                            name = user.displayName ?: "",
                            email = user.email ?: ""
                        )
                        Log.d(TAG, "User created, needs onboarding = true")
                        return Result.success(true) // Needs onboarding
                    } else {
                        val needsOnboarding = !existingUser.onboardingCompleted
                        Log.d(TAG, "Existing user, onboardingCompleted: ${existingUser.onboardingCompleted}, needsOnboarding: $needsOnboarding")
                        return Result.success(needsOnboarding)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore error", e)
                    // If Firestore fails, assume new user needs onboarding
                    return Result.success(true)
                }
            }
            
            return Result.failure(Exception("Authentication failed - no user"))
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Token parsing error", e)
            return Result.failure(e)
        }
    }
    
    Log.e(TAG, "Invalid credential type: ${credential.type}")
    return Result.failure(Exception("Invalid credential type"))
}
