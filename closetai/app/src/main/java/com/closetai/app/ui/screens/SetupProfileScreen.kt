package com.closetai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.closetai.app.data.api.ApiClient
import com.closetai.app.data.model.RecommendationRequest
import com.closetai.app.data.model.toBackendUserProfile
import com.closetai.app.data.repository.RecommenderRepository
import com.closetai.app.data.repository.UserRepository
import com.closetai.app.data.repository.WardrobeRepository
import com.closetai.app.data.settings.ServerConfigStore
import com.closetai.app.data.settings.UserProfileCacheStore
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

@Composable
fun SetupProfileScreen(
    onReady: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }
    val wardrobeRepository = remember { WardrobeRepository() }
    val recommenderRepository = remember { RecommenderRepository(ApiClient.closetAiApi) }

    var statusText by remember { mutableStateOf("Setting up your profile…") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryNonce by remember { mutableStateOf(0) }
    var serverUrl by remember { mutableStateOf("") }
    var showServerConfig by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        serverUrl = ServerConfigStore.getBaseUrl(context)?.trim().orEmpty()
    }

    fun normalizeForSaving(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ""
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    fun saveServerUrlAndRetry() {
        val normalized = normalizeForSaving(serverUrl)
        if (normalized.isBlank()) {
            error = "Please enter a valid server URL or IP (example: 192.168.1.10:8081)"
            showServerConfig = true
            return
        }
        ServerConfigStore.setBaseUrl(context, normalized)
        ApiClient.configureBaseUrl(normalized)
        serverUrl = normalized
        error = null
        retryNonce += 1
    }

    suspend fun warmup(): Boolean {
        statusText = "Setting up your profile…"
        error = null
        isLoading = true

        val user = userRepository.getCurrentUser()
        if (user == null) {
            error = "User not found. Please sign in again."
            isLoading = false
            return false
        }

        statusText = "Preparing your wardrobe…"
        val wardrobeItems = try {
            withTimeout(12_000) {
                val wardrobeResult = wardrobeRepository.fetchWardrobeItems()
                wardrobeResult.getOrDefault(emptyList())
            }
        } catch (_: TimeoutCancellationException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val backendProfile = user.toBackendUserProfile(wardrobeItems)
        UserProfileCacheStore.setBackendProfile(context, backendProfile)

        statusText = "Finding good fits for you…"
        val request = RecommendationRequest(
            userProfile = backendProfile,
            context = emptyMap(),
            offset = 0,
            limit = 20
        )

        // Retry once in case scraping/network hiccups during demo.
        repeat(2) { attempt ->
            val result = try {
                withTimeout(18_000) {
                    recommenderRepository.getRecommendations(request)
                }
            } catch (_: TimeoutCancellationException) {
                Result.failure(Exception("Request timed out. Check your server URL/IP and that the backend is running."))
            } catch (e: Exception) {
                Result.failure(e)
            }
            val ok = result.isSuccess && (result.getOrNull()?.isNotEmpty() == true)
            if (ok) {
                isLoading = false
                return true
            }
            if (attempt == 0) delay(900)
        }

        error = "Couldn't reach the backend. Set your server URL/IP and retry."
        showServerConfig = true
        isLoading = false
        return false
    }

    LaunchedEffect(retryNonce) {
        showServerConfig = false
        val ok = warmup()
        if (ok) onReady()
    }

    // If we're "stuck" loading for a while, proactively show server config.
    LaunchedEffect(isLoading, statusText) {
        if (!isLoading) return@LaunchedEffect
        delay(7_000)
        if (isLoading) showServerConfig = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This makes your recommendations instant.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showServerConfig) {
            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Backend server URL",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Set your server IP so the app can reach the backend.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("e.g. http://192.168.1.10:8081/") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                enabled = !isLoading,
                onClick = { saveServerUrlAndRetry() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Retry")
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now")
            }
        }

        error?.let { msg ->
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                enabled = !isLoading,
                onClick = {
                    error = null
                    retryNonce += 1
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retry")
            }
        }
    }
}

