package com.closetai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.closetai.app.ui.theme.Primary

@Composable
fun ColorSwatch(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatchShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = if (isSelected) 6.dp else 2.dp,
                    shape = swatchShape,
                    ambientColor = if (isSelected) Primary.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f),
                    spotColor = if (isSelected) Primary.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
                )
                .clip(swatchShape)
                .background(color)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = when {
                        isSelected -> Primary
                        isColorLight(color) -> Color.Gray.copy(alpha = 0.4f)
                        else -> Color.White.copy(alpha = 0.15f)
                    },
                    shape = swatchShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                val iconTint = if (isColorLight(color)) Color.Black else Color.White

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (isColorLight(color)) Color.Black.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 60.dp)
        )
    }
}

// Simple helper to determine if a color is light or dark
private fun isColorLight(color: Color): Boolean {
    val luminance = 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue
    return luminance > 0.5
}
