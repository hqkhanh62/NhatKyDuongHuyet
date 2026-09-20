package com.example.nhatkyduonghuyet.ui.scanner.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Professional AI alignment frame for the Pro scanner:
 *
 * - Dims everything outside the targeting window so the meter display pops.
 * - Draws glowing corner brackets (green = scanning, red = danger reading).
 * - Animates a laser scan-line while [scanning] is true.
 */
@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    scanning: Boolean = true,
    isDanger: Boolean = false,
    frameWidth: Dp = 300.dp,
    frameHeight: Dp = 210.dp,
    cornerLength: Dp = 30.dp
) {
    val density = LocalDensity.current

    val transition = rememberInfiniteTransition(label = "scanline")
    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanProgress"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val frameWpx = with(density) { minOf(frameWidth, maxWidth - 48.dp).toPx() }
        val frameHpx = with(density) { frameHeight.toPx() }
        val cornerPx = with(density) { cornerLength.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val left = (canvasW - frameWpx) / 2f
            // Slightly above center: leaves room for the bottom result banner.
            val top = (canvasH - frameHpx) / 2f - 40.dp.toPx()
            val frame = Rect(left, top, left + frameWpx, top + frameHpx)

            // 1. Dim outside the frame.
            val dimPath = Path().apply {
                addRect(Rect(0f, 0f, canvasW, canvasH))
                addRoundRect(
                    RoundRect(frame, CornerRadius(24.dp.toPx()))
                )
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            }
            drawPath(dimPath, Color.Black.copy(alpha = 0.55f))

            val accent = if (isDanger) Color(0xFFFF5252) else Color(0xFF00E676)

            // 2. Corner brackets.
            val stroke = 5.dp.toPx()
            val inset = 2.dp.toPx()
            val corners = listOf(
                // top-left
                Offset(frame.left + inset, frame.top + inset) to
                    listOf(Offset(1f, 0f), Offset(0f, 1f)),
                // top-right
                Offset(frame.right - inset, frame.top + inset) to
                    listOf(Offset(-1f, 0f), Offset(0f, 1f)),
                // bottom-left
                Offset(frame.left + inset, frame.bottom - inset) to
                    listOf(Offset(1f, 0f), Offset(0f, -1f)),
                // bottom-right
                Offset(frame.right - inset, frame.bottom - inset) to
                    listOf(Offset(-1f, 0f), Offset(0f, -1f))
            )
            corners.forEach { (origin, directions) ->
                directions.forEach { dir ->
                    drawLine(
                        color = accent,
                        start = origin,
                        end = origin + Offset(dir.x * cornerPx, dir.y * cornerPx),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Thin frame outline for extra structure.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(frame.left, frame.top),
                size = Size(frameWpx, frameHpx),
                cornerRadius = CornerRadius(24.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Animated laser scan-line.
            if (scanning) {
                val lineY = frame.top + 12.dp.toPx() +
                    scanProgress * (frameHpx - 24.dp.toPx())
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.9f),
                            Color.Transparent
                        ),
                        startX = frame.left,
                        endX = frame.right
                    ),
                    start = Offset(frame.left + 16.dp.toPx(), lineY),
                    end = Offset(frame.right - 16.dp.toPx(), lineY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Soft glow under the line.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(frame.left + 16.dp.toPx(), lineY),
                    size = Size(frameWpx - 32.dp.toPx(), 28.dp.toPx())
                )
            } else {
                // Locked state: solid accent border pulse feel.
                drawRoundRect(
                    color = accent.copy(alpha = 0.8f),
                    topLeft = Offset(frame.left, frame.top),
                    size = Size(frameWpx, frameHpx),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }
    }
}
