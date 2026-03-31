package com.closetai.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.closetai.app.data.api.ApiClient
import com.closetai.app.data.model.ProductRecommendation
import com.closetai.app.data.repository.RecommenderRepository
import com.closetai.app.data.settings.SavedItemsStore
import com.closetai.app.data.settings.TryOnUserImageStore
import com.closetai.app.data.settings.TryOnHistoryItem
import com.closetai.app.data.settings.TryOnHistoryStore
import com.closetai.app.ui.viewmodel.RecommendationsUiState
import com.closetai.app.ui.viewmodel.RecommendationsViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "RecommendationsScreen"
private const val IMAGE_PLACEHOLDER_URL = "https://via.placeholder.com/600x800.png?text=ClosetAI"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    viewModel: RecommendationsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateSaved: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val likedIds by viewModel.likedProductIds.collectAsState()
    var selectedRecommendation by remember { mutableStateOf<ProductRecommendation?>(null) }
    var tryOnLoading by remember { mutableStateOf(false) }
    var tryOnDialogOpen by remember { mutableStateOf(false) }
    var tryOnStatusText by remember { mutableStateOf("Your try-on is getting generated…") }
    var tryOnResultFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    val recommenderRepository = remember { RecommenderRepository(ApiClient.closetAiApi) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                try {
                    val dst = File(context.filesDir, "tryon_user.jpg")
                    context.contentResolver.openInputStream(it)?.use { input ->
                        dst.outputStream().use { output -> input.copyTo(output) }
                    }
                    TryOnUserImageStore.setUserImagePath(context, dst.absolutePath)
                    Toast.makeText(context, "Photo saved for try-on", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Recommendations") },
                actions = {
                    IconButton(onClick = onNavigateSaved) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Saved items")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is RecommendationsUiState.Initial,
                is RecommendationsUiState.Loading -> {
                    RecommendationsLoadingSkeleton(
                        message = (state as? RecommendationsUiState.Loading)?.message
                            ?: "Finding good for you…",
                        placeholders = (state as? RecommendationsUiState.Loading)?.placeholders ?: 6
                    )
                }
                is RecommendationsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retryLastFetch() }) {
                            Text("Retry")
                        }
                    }
                }
                is RecommendationsUiState.Success -> {
                    if (state.recommendations.isEmpty()) {
                        Text(
                            "No recommendations found.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        RecommendationsGrid(
                            recommendations = state.recommendations,
                            isLoadingMore = state.isLoadingMore,
                            canLoadMore = state.canLoadMore,
                            onLoadMore = { viewModel.loadMore() },
                            likedIds = likedIds,
                            onToggleLike = { rec, liked ->
                                val uid = FirebaseAuth.getInstance().currentUser?.uid
                                if (!uid.isNullOrBlank()) {
                                    if (liked) {
                                        SavedItemsStore.upsertItem(context, uid, rec)
                                    } else {
                                        SavedItemsStore.removeItem(context, uid, rec.id)
                                    }
                                }
                                viewModel.toggleLike(rec, liked)
                            },
                            onOpenPreview = { rec -> selectedRecommendation = rec }
                        )
                    }
                }
            }
        }
    }

    selectedRecommendation?.let { recommendation ->
        RecommendationPreviewDialog(
            recommendation = recommendation,
            tryOnLoading = tryOnLoading,
            onTryOn = { rec ->
                val storedPath = TryOnUserImageStore.getUserImagePath(context)
                if (storedPath.isNullOrBlank()) {
                    imagePicker.launch("image/*")
                    Toast.makeText(context, "Upload your photo once to try-on", Toast.LENGTH_SHORT).show()
                    return@RecommendationPreviewDialog
                }

                // Run generation in background coroutine.
                tryOnLoading = true
                tryOnDialogOpen = true
                tryOnStatusText = "Your try-on is getting generated…"
                tryOnResultFile = null
                val tmp = File(storedPath)
                if (!tmp.exists() || tmp.length() <= 0) {
                    tryOnLoading = false
                    TryOnUserImageStore.clear(context)
                    Toast.makeText(context, "Saved photo missing. Pick again.", Toast.LENGTH_SHORT).show()
                    imagePicker.launch("image/*")
                    return@RecommendationPreviewDialog
                }

                scope.launch(Dispatchers.IO) {
                    val res = recommenderRepository.tryOn(
                        userImageFile = tmp,
                        garmentImageUrl = rec.imageUrl.toSafeImageUrl(),
                        garmentDes = rec.title
                    )
                    val outFile = if (res.isSuccess) {
                        try {
                            val b64 = res.getOrNull()?.imageBase64.orEmpty()
                            val bytes = Base64.getDecoder().decode(b64)
                            val f = File.createTempFile("tryon_out_", ".png", context.cacheDir)
                            f.writeBytes(bytes)
                            f
                        } catch (_: Exception) {
                            null
                        }
                    } else null

                    withContext(Dispatchers.Main) {
                        tryOnLoading = false
                        if (outFile != null) {
                            tryOnResultFile = outFile
                            TryOnHistoryStore.add(
                                context,
                                TryOnHistoryItem(
                                    createdAt = System.currentTimeMillis(),
                                    title = rec.title,
                                    outputPath = outFile.absolutePath
                                )
                            )
                            Toast.makeText(context, "Try-on ready", Toast.LENGTH_SHORT).show()
                        } else {
                            tryOnStatusText = "Try-on failed. Please try again."
                            Toast.makeText(context, "Try-on failed. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDismiss = { selectedRecommendation = null }
        )
    }

    if (tryOnDialogOpen) {
        TryOnGenerationDialog(
            isGenerating = tryOnLoading,
            statusText = tryOnStatusText,
            imageFile = tryOnResultFile,
            onDismiss = {
                tryOnDialogOpen = false
                tryOnResultFile = null
                tryOnLoading = false
            }
        )
    }
}

@Composable
private fun RecommendationsLoadingSkeleton(
    message: String,
    placeholders: Int
) {
    val infinite = rememberInfiniteTransition(label = "skeleton")
    val pulse by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items((0 until placeholders).toList()) {
                SkeletonRecommendationCard(alpha = pulse)
            }
        }
    }
}

@Composable
private fun SkeletonRecommendationCard(alpha: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.74f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f * alpha))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f * alpha))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f * alpha))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f * alpha))
                )
            }
        }
    }
}

@Composable
fun RecommendationsGrid(
    recommendations: List<ProductRecommendation>,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    likedIds: Set<String>,
    onToggleLike: (ProductRecommendation, Boolean) -> Unit,
    onOpenPreview: (ProductRecommendation) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(recommendations) { item ->
                RecommendationCard(
                    recommendation = item,
                    isLiked = likedIds.contains(item.id),
                    onToggleLike = { liked -> onToggleLike(item, liked) },
                    onClick = { onOpenPreview(item) }
                )
            }
        }

        if (canLoadMore) {
            Surface(tonalElevation = 2.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onLoadMore,
                        enabled = !isLoadingMore
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Loading…")
                        } else {
                            Text("Load more")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationPreviewDialog(
    recommendation: ProductRecommendation,
    tryOnLoading: Boolean,
    onTryOn: (ProductRecommendation) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = recommendation.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                coil.compose.AsyncImage(
                    model = recommendation.imageUrl.toSafeImageUrl(),
                    contentDescription = recommendation.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    placeholder = ColorPainter(Color.LightGray),
                    error = ColorPainter(Color.Gray)
                )

                Text(
                    text = "${recommendation.brand} • ₹${recommendation.price.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Confidence: ${(recommendation.matchScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                recommendation.matchReasons.take(3).forEach { reason ->
                    if (reason.isNotBlank()) {
                        Text(
                            text = "• $reason",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    enabled = !tryOnLoading,
                    onClick = { onTryOn(recommendation) }
                ) {
                    if (tryOnLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Generating…")
                    } else {
                        Text("Try On")
                    }
                }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recommendation.productUrl))
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Link")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// (Replaced by TryOnGenerationDialog)

@Composable
private fun TryOnGenerationDialog(
    isGenerating: Boolean,
    statusText: String,
    imageFile: File?,
    onDismiss: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "tryon_skeleton")
    val pulse by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tryon_pulse"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Try-on") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (imageFile != null) {
                    coil.compose.AsyncImage(
                        model = imageFile,
                        contentDescription = "Try-on result",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        placeholder = ColorPainter(Color.LightGray),
                        error = ColorPainter(Color.Gray)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f * pulse))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                enabled = !isGenerating || imageFile != null
            ) {
                Text(if (imageFile != null) "Done" else "Close")
            }
        }
    )
}

@Composable
fun RecommendationCard(
    recommendation: ProductRecommendation,
    isLiked: Boolean,
    onToggleLike: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.74f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Load image with Coil AsyncImage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.LightGray)
            ) {
                val imageModel = recommendation.imageUrl
                    .toSafeImageUrl()

                coil.compose.AsyncImage(
                    model = imageModel,
                    contentDescription = recommendation.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    placeholder = ColorPainter(Color.LightGray),
                    error = ColorPainter(Color.Gray),
                    onError = { state ->
                        Log.e(
                            TAG,
                            "Image load failed for url=${recommendation.imageUrl}: ${state.result.throwable.message}"
                        )
                    }
                )
                // Score badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${(recommendation.matchScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = { onToggleLike(!isLiked) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = recommendation.brand,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "₹${recommendation.price.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                val reasons = recommendation.matchReasons.take(2).filter { it.isNotBlank() }
                if (reasons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reasons.forEach { reason ->
                            AssistChip(
                                onClick = { /* informational */ },
                                label = {
                                    Text(
                                        text = reason,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String?.toSafeImageUrl(): String {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) {
        return IMAGE_PLACEHOLDER_URL
    }
    return if (raw.startsWith("https://") || raw.startsWith("http://")) {
        raw
    } else {
        IMAGE_PLACEHOLDER_URL
    }
}
