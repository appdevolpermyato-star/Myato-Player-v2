package com.example.ui.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.*

/**
 * Official Myato Player Street Culture Circular MP Logo.
 * Uses the official Street Art graffiti MP artwork with purple glowing border.
 */
@Composable
fun MpStreetLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showGlow: Boolean = true
) {
    val glowModifier = if (showGlow) {
        modifier.shadow(12.dp, CircleShape, spotColor = PurplePrimary)
    } else {
        modifier
    }

    Box(
        modifier = glowModifier
            .size(size)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                brush = Brush.sweepGradient(
                    listOf(
                        PurplePrimary,
                        PurpleBright,
                        PurpleIntense,
                        PurplePrimary
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.myato_logo),
            contentDescription = "Myato Player MP",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
