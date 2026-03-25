package com.closetai.app.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.airbnb.lottie.compose.*
import com.closetai.app.R
import com.closetai.app.data.repository.UserRepository
import com.closetai.app.ui.components.GlassmorphicCard
import com.closetai.app.ui.theme.BackgroundDark
import com.closetai.app.ui.theme.Primary
import com.closetai.app.ui.theme.PrimaryDark
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

private const val SPLASH_TAG = "SplashScreen"

private enum class SplashPhase { FADE_IN, CENTERED, SLIDING, LOGIN }

@Composable
fun SplashScreen(
    onNavigateToSignIn: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var phase by remember { mutableStateOf(SplashPhase.FADE_IN) }
    var showLogin by remember { mutableStateOf(false) }     // once true, login card mounts
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isDarkTheme = isSystemInDarkTheme()

    // ── Animation values ──────────────────────────────────────────────────────

    // Logo fade-in
    val logoAlpha by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.FADE_IN -> 0f
            SplashPhase.CENTERED -> 1f
            SplashPhase.SLIDING, SplashPhase.LOGIN -> 0f   // fade OUT while sliding up
        },
        animationSpec = tween(
            durationMillis = when (phase) {
                SplashPhase.FADE_IN -> 800
                SplashPhase.CENTERED -> 800
                else -> 500   // fade matches the slide duration
            },
            easing = FastOutSlowInEasing
        ),
        label = "logoAlpha"
    )

    // Logo slides from center → top (uses fraction 0f=center, 1f=top-area)
    val slideProgress by animateFloatAsState(
        targetValue = if (phase == SplashPhase.SLIDING || phase == SplashPhase.LOGIN) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "slideProgress"
    )

    // Login card: fades + slides in from below
    val loginReveal by animateFloatAsState(
        targetValue = if (phase == SplashPhase.LOGIN) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "loginReveal"
    )

    // ── Phase sequencer ───────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            phase = SplashPhase.CENTERED
            delay(1500)
            scope.launch {
                val done = userRepository.isOnboardingCompleted()
                if (done) onNavigateToHome() else onNavigateToOnboarding()
            }
        } else {
            phase = SplashPhase.CENTERED
            delay(1800)
            phase = SplashPhase.SLIDING
            showLogin = true             // mount login NOW so it's ready
            delay(600)
            phase = SplashPhase.LOGIN
        }
    }

    // ── Lottie composition ────────────────────────────────────────────────────
    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.wardrobe)
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = LottieConstants.IterateForever
    )

    // ── Root layout ───────────────────────────────────────────────────────────
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDark,
                        Color(0xFF2E1218),
                        Color(0xFF1A0C10)
                    )
                )
            )
    ) {
        val screenHeight = constraints.maxHeight.toFloat()  // pixels

        // Decorative glows
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset(x = (-60).dp, y = (-80).dp)
                    .size(300.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Primary.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = 60.dp)
                    .size(260.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(PrimaryDark.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
            )
        }

        // ── LOGO BLOCK ────────────────────────────────────────────────────────
        // slideProgress 0 = vertically centered, 1 = near top
        // When centered: translateY = 0 (Box align=Center handles centering)
        // When sliding up: translateY = negative (upward in px, convert to dp)
        val density = LocalContext.current.resources.displayMetrics.density
        val centerY = 0f
        // Target top position: we want the logo at ~18% from top screen
        // Center is at 50% → move up by ~30% of screen height
        val slideUpDp = (screenHeight * 0.32f * slideProgress / density).dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -slideUpDp)   // negative = moves UP
                .alpha(logoAlpha)
                .zIndex(0f)
                .padding(horizontal = 32.dp)
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Primary.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        CircleShape
                    )
                    .border(1.5.dp, Primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lottie wardrobe animation
            LottieAnimation(
                composition = lottieComposition,
                progress = { lottieProgress },
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Wordmark
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Closet",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = (-1.5).sp
                )
                Text(
                    text = "AI",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    letterSpacing = (-1.5).sp
                )
                Box(
                    modifier = Modifier
                        .padding(start = 3.dp, bottom = 8.dp)
                        .size(7.dp)
                        .background(Primary, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your AI-Powered Wardrobe",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.45f),
                letterSpacing = 0.5.sp
            )
        }

        // ── LOGIN CARD — shown in CENTER of screen ────────────────────────────
        if (showLogin) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .alpha(loginReveal)
                    .offset(y = ((1f - loginReveal) * 60).dp)   // slides up 60dp as it reveals
                    .zIndex(1f)                                  // always on TOP of logo
            ) {
                // Small top logo (replaces the big centered one after slide)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checkroom,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "ClosetAI",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Welcome text
                Text(
                    text = "Welcome to your",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = "digital closet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Glassmorphic sign-in card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sign in to curate your style, organize your wardrobe, and get AI-powered outfit suggestions.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SplashGoogleSignInButton(
                            isLoading = isLoading,
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        val result = splashSignInWithGoogle(context)
                                        result.onSuccess { needsOnboarding ->
                                            if (needsOnboarding) onNavigateToOnboarding()
                                            else onNavigateToHome()
                                        }.onFailure { e ->
                                            Log.e(SPLASH_TAG, "Sign-in failed", e)
                                            errorMessage = e.message ?: "Sign in failed"
                                            isLoading = false
                                        }
                                    } catch (e: Exception) {
                                        Log.e(SPLASH_TAG, "Exception", e)
                                        errorMessage = e.message ?: "Sign in failed"
                                        isLoading = false
                                    }
                                }
                            }
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "By continuing you agree to our Terms & Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Powered-by footer (splash phase only)
        Text(
            text = "POWERED BY FASHION INTELLIGENCE",
            style = MaterialTheme.typography.labelSmall,
            color = Primary.copy(alpha = 0.45f),
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(1f - loginReveal)
        )
    }
}

// ── Sign-In Button ────────────────────────────────────────────────────────────

@Composable
private fun SplashGoogleSignInButton(
    isLoading: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isDarkTheme) Color(0xFF2A1519) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1F1F1F)
    val border = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color(0xFFDADCE0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Primary)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("G", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                Text(
                    "Continue with Google",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

// ── Google Sign-In Logic ──────────────────────────────────────────────────────

private fun generateNonce(): String {
    val rawNonce = UUID.randomUUID().toString()
    val bytes = rawNonce.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

private suspend fun splashSignInWithGoogle(context: Context): Result<Boolean> {
    val credentialManager = CredentialManager.create(context)
    val auth = FirebaseAuth.getInstance()
    val userRepository = UserRepository()
    val webClientId = context.getString(R.string.default_web_client_id)
    val nonce = generateNonce()

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(true)
        .setNonce(nonce)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val result = credentialManager.getCredential(context, request)
        splashHandleResult(result, auth, userRepository)
    } catch (e: GetCredentialException) {
        Log.e(SPLASH_TAG, "getCredential failed: ${e.type} - ${e.message}", e)
        val msg = when {
            e.message?.contains("No credentials", ignoreCase = true) == true ||
            e.message?.contains("No Google accounts", ignoreCase = true) == true ->
                "No Google account found. Sign in to a Google account on your device first."
            e.message?.contains("Cancel", ignoreCase = true) == true ->
                "Sign-in cancelled."
            else -> "Sign-in failed: ${e.message}"
        }
        Result.failure(Exception(msg))
    } catch (e: Exception) {
        Log.e(SPLASH_TAG, "Unexpected error", e)
        Result.failure(Exception("Sign-in failed: ${e.message}"))
    }
}

private suspend fun splashHandleResult(
    result: GetCredentialResponse,
    auth: FirebaseAuth,
    userRepository: UserRepository
): Result<Boolean> {
    val credential = result.credential
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        return try {
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val authResult = auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
            val user = authResult.user ?: return Result.failure(Exception("Auth failed"))
            val existing = try { userRepository.getCurrentUser() } catch (e: Exception) { null }
            if (existing == null) {
                userRepository.createUser(name = user.displayName ?: "", email = user.email ?: "")
                Result.success(true)
            } else {
                Result.success(!existing.onboardingCompleted)
            }
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(e)
        }
    }
    return Result.failure(Exception("Invalid credential type"))
}
