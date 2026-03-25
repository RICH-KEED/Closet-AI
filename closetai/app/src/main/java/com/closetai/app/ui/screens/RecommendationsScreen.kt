package com.closetai.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.closetai.app.data.model.ProductRecommendation
import com.closetai.app.ui.viewmodel.RecommendationsUiState
import com.closetai.app.ui.viewmodel.RecommendationsViewModel

private const val TAG = "RecommendationsScreen"
private const val IMAGE_PLACEHOLDER_URL = "https://via.placeholder.com/600x800.png?text=ClosetAI"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    viewModel: RecommendationsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Recommendations") }
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                        RecommendationsGrid(recommendations = state.recommendations)
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationsGrid(recommendations: List<ProductRecommendation>) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(recommendations) { item ->
            RecommendationCard(
                recommendation = item,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.productUrl))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun RecommendationCard(
    recommendation: ProductRecommendation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Load image with Coil AsyncImage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
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
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Score: ${recommendation.matchScore.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = recommendation.brand,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "₹${recommendation.price.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
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
