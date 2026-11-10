package com.techressive.hex.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.techressive.hex.metadata.Track
import com.techressive.hex.ui.theme.HexColors
import com.techressive.hex.ui.theme.HexLogo
import java.io.File

@Composable
fun AlbumCoverView(
    track: Track?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    showFetchButton: Boolean = true,
    onFetchArtwork: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val artUri = track?.artworkUri
    val hasArtwork = artUri != null && artUri.toString().isNotBlank()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(cornerRadius), ambientColor = HexColors.CobaltBlue.copy(alpha = 0.4f), spotColor = HexColors.Lavender.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(HexColors.DarkGray)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (hasArtwork) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = track?.album ?: "Album Art",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ProceduralVinylCover()
            }
        }

        // "Fetch artwork" action button if no artwork exists
        if (!hasArtwork && showFetchButton && onFetchArtwork != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(HexColors.CobaltBlue.copy(alpha = 0.2f))
                    .border(1.dp, HexColors.CobaltBlue.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable { onFetchArtwork() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Fetch artwork",
                        tint = HexColors.Lavender,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Fetch artwork",
                        color = HexColors.Lavender,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ProceduralVinylCover(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1B1D24), Color(0xFF070709), HexColors.PureBlack)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f

            // Concentric vinyl grooves
            for (r in 30 until maxR.toInt() step 12) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = r.toFloat(),
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // Center vinyl label
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(HexColors.CobaltBlue.copy(alpha = 0.8f), HexColors.Lavender.copy(alpha = 0.6f))
                ),
                radius = maxR * 0.32f,
                center = center
            )

            // Spindle hole
            drawCircle(
                color = HexColors.PureBlack,
                radius = maxR * 0.08f,
                center = center
            )
            drawCircle(
                color = HexColors.SlateGray.copy(alpha = 0.6f),
                radius = maxR * 0.08f,
                center = center,
                style = Stroke(width = 2f)
            )
        }

        // Centered mini HEX logo
        HexLogo(size = 28.dp)
    }
}
