package com.techressive.hex.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techressive.hex.ui.theme.HexColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HexSplashScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillar1Progress = remember { Animatable(0f) }
    val pillar2Progress = remember { Animatable(0f) }
    val pillar3Progress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Pillar 1 animation
        launch {
            pillar1Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        // Pillar 2 animation (staggered delay)
        launch {
            delay(180)
            pillar2Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        // Pillar 3 animation (staggered delay)
        launch {
            delay(360)
            pillar3Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        // Wordmark and engine title fade-in
        launch {
            delay(580)
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }

        // Completion delay
        delay(1250)
        onFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HexColors.OledBlack)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onFinish() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated 3-Pillar Logo Canvas
            Canvas(modifier = Modifier.size(110.dp)) {
                val w = size.width
                val h = size.height

                // Pillar 1: Left vertical pillar
                val p1 = pillar1Progress.value
                if (p1 > 0f) {
                    val offsetY = (1f - p1) * 24f
                    translate(left = 0f, top = offsetY) {
                        val path1 = Path().apply {
                            moveTo(w * 0.22f, h * 0.25f)
                            lineTo(w * 0.36f, h * 0.17f)
                            lineTo(w * 0.36f, h * 0.83f)
                            lineTo(w * 0.22f, h * 0.75f)
                            close()
                        }
                        drawPath(path1, brush = HexColors.BrandGradient, alpha = p1)
                    }
                }

                // Pillar 2: Center-Right pillar
                val p2 = pillar2Progress.value
                if (p2 > 0f) {
                    val offsetY = (1f - p2) * 24f
                    translate(left = 0f, top = offsetY) {
                        val path2 = Path().apply {
                            moveTo(w * 0.43f, h * 0.35f)
                            lineTo(w * 0.57f, h * 0.27f)
                            lineTo(w * 0.57f, h * 0.93f)
                            lineTo(w * 0.43f, h * 0.85f)
                            close()
                        }
                        drawPath(path2, brush = HexColors.BrandGradient, alpha = p2)
                    }
                }

                // Pillar 3: Right / Header pillar
                val p3 = pillar3Progress.value
                if (p3 > 0f) {
                    val offsetY = (1f - p3) * 24f
                    translate(left = 0f, top = offsetY) {
                        val path3 = Path().apply {
                            moveTo(w * 0.64f, h * 0.15f)
                            lineTo(w * 0.78f, h * 0.07f)
                            lineTo(w * 0.78f, h * 0.73f)
                            lineTo(w * 0.64f, h * 0.65f)
                            close()
                        }
                        drawPath(path3, brush = HexColors.ReverseBrandGradient, alpha = p3)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Wordmark
            Text(
                text = "hex",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = HexColors.PureWhite.copy(alpha = textAlpha.value),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "AUDIOPHILE ENGINE",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = HexColors.Lavender.copy(alpha = textAlpha.value * 0.9f),
                letterSpacing = 3.sp
            )
        }
    }
}
