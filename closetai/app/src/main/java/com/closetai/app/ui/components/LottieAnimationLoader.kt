package com.closetai.app.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.closetai.app.R

@Composable
fun GenderAnimation(
    gender: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val animationRes = when (gender.lowercase()) {
        "female" -> R.raw.female
        else -> R.raw.male
    }
    
    // Male animation is 1920x1080 (wide), Female is 1000x1000 (square)
    // Scale male up to match female's visual size
    val displaySize = when (gender.lowercase()) {
        "male" -> size * 1.4f
        else -> size
    }
    
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(animationRes)
    )
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(displaySize)
    )
}

@Composable
fun LottieAnimationView(
    @RawRes animationRes: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    iterations: Int = LottieConstants.IterateForever
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(animationRes)
    )
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations
    )
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size)
    )
}
