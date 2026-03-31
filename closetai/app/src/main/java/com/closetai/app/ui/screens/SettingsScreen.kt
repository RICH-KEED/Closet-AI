package com.closetai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.closetai.app.data.api.ApiClient
import com.closetai.app.data.settings.ServerConfigStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf("") }
    var serverUrlError by remember { mutableStateOf<String?>(null) }

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

    fun validate(input: String): String? {
        val v = input.trim()
        if (v.isBlank()) return "Server URL can't be empty"
        if (!v.startsWith("http://") && !v.startsWith("https://") && !v.contains(".")) {
            // Basic guard for accidental junk like "abcd"
            return "Enter a valid URL or IP (example: 192.168.1.10:8081)"
        }
        return null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PaddingValues(horizontal = 20.dp, vertical = 16.dp)),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Backend server URL",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Change this when your Wi‑Fi IP changes. Include port if needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    serverUrlError = null
                },
                label = { Text("Server URL (e.g. http://192.168.1.10:8081/)") },
                isError = serverUrlError != null,
                supportingText = {
                    serverUrlError?.let { Text(it) }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val err = validate(serverUrl)
                    if (err != null) {
                        serverUrlError = err
                        return@Button
                    }

                    val normalized = normalizeForSaving(serverUrl)
                    ServerConfigStore.setBaseUrl(context, normalized)
                    ApiClient.configureBaseUrl(normalized)

                    scope.launch {
                        snackbarHostState.showSnackbar("Saved: $normalized")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            TextButton(
                onClick = {
                    serverUrl = ""
                    serverUrlError = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Cleared. App will use default on next launch.")
                    }
                }
            ) {
                Text("Clear saved URL")
            }
        }
    }
}

