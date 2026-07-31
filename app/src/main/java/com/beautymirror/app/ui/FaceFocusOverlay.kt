package com.beautymirror.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.beautymirror.app.tracking.FaceTrackingResult
import com.beautymirror.app.ui.theme.BmAccent

/**
 * Non-interactive, face-anchored visual feedback for the currently edited region.
 * Controls stay in a face-safe sheet; only these lightweight outlines touch the preview.
 */
@Composable
fun FaceFocusOverlay(
    tracking: FaceTrackingResult,
    focus: BeautyFocus,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible || !tracking.isValid || tracking.displayLandmarks.isEmpty()) return
    val accent = BmAccent
    Canvas(modifier.fillMaxSize()) {
        val points = tracking.displayLandmarks
        fun point(index: Int): Offset? = points.getOrNull(index)?.let {
            Offset(it.x * size.width, it.y * size.height)
        }
        fun midpoint(a: Int, b: Int): Offset? {
            val p = point(a) ?: return null
            val q = point(b) ?: return null
            return Offset((p.x + q.x) * 0.5f, (p.y + q.y) * 0.5f)
        }

        val bounds = tracking.bounds
        val faceRect = Rect(
            left = bounds.left * size.width,
            top = bounds.top * size.height,
            right = bounds.right * size.width,
            bottom = bounds.bottom * size.height,
        )
        val faceWidth = faceRect.width.coerceAtLeast(size.width * 0.12f)
        val faceHeight = faceRect.height.coerceAtLeast(size.height * 0.16f)
        val line = (size.minDimension * 0.0032f).coerceIn(2f, 5f)
        val soft = accent.copy(alpha = 0.22f)
        val strong = accent.copy(alpha = 0.78f)

        fun ring(center: Offset, rx: Float, ry: Float, alpha: Float = 0.72f) {
            drawOval(
                color = accent.copy(alpha = alpha),
                topLeft = Offset(center.x - rx, center.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = line),
            )
        }

        when (focus) {
            BeautyFocus.OVERVIEW -> {
                drawOval(soft, faceRect.topLeft, faceRect.size)
                drawOval(strong, faceRect.topLeft, faceRect.size, style = Stroke(line))
            }

            BeautyFocus.SKIN -> {
                drawOval(soft, faceRect.topLeft, faceRect.size)
                drawOval(strong, faceRect.topLeft, faceRect.size, style = Stroke(line))
                midpoint(33, 263)?.let { center ->
                    ring(center, faceWidth * 0.33f, faceHeight * 0.18f, 0.35f)
                }
            }

            BeautyFocus.EYES -> {
                midpoint(33, 133)?.let { ring(it, faceWidth * 0.17f, faceHeight * 0.075f) }
                midpoint(362, 263)?.let { ring(it, faceWidth * 0.17f, faceHeight * 0.075f) }
                midpoint(33, 133)?.let { left ->
                    drawArc(
                        color = strong,
                        startAngle = 8f,
                        sweepAngle = 164f,
                        useCenter = false,
                        topLeft = Offset(left.x - faceWidth * 0.18f, left.y),
                        size = Size(faceWidth * 0.36f, faceHeight * 0.17f),
                        style = Stroke(line),
                    )
                }
                midpoint(362, 263)?.let { right ->
                    drawArc(
                        color = strong,
                        startAngle = 8f,
                        sweepAngle = 164f,
                        useCenter = false,
                        topLeft = Offset(right.x - faceWidth * 0.18f, right.y),
                        size = Size(faceWidth * 0.36f, faceHeight * 0.17f),
                        style = Stroke(line),
                    )
                }
            }

            BeautyFocus.LIPS -> {
                midpoint(13, 14)?.let { ring(it, faceWidth * 0.19f, faceHeight * 0.07f) }
            }

            BeautyFocus.SHAPE -> {
                val path = Path()
                val jaw = intArrayOf(234, 172, 136, 150, 176, 152, 400, 379, 365, 397, 454)
                var first = true
                for (index in jaw) {
                    val p = point(index) ?: continue
                    if (first) {
                        path.moveTo(p.x, p.y)
                        first = false
                    } else {
                        path.lineTo(p.x, p.y)
                    }
                }
                drawPath(path, strong, style = Stroke(line * 1.15f))
                point(1)?.let { ring(it, faceWidth * 0.08f, faceHeight * 0.16f, 0.52f) }
            }

            BeautyFocus.SCENE -> {
                repeat(3) { index ->
                    val y = faceRect.center.y + faceHeight * (0.17f + index * 0.10f)
                    val path = Path().apply {
                        moveTo(faceRect.left - faceWidth * 0.16f, y)
                        cubicTo(
                            faceRect.left + faceWidth * 0.18f,
                            y - faceHeight * 0.035f,
                            faceRect.center.x,
                            y + faceHeight * 0.035f,
                            faceRect.right + faceWidth * 0.16f,
                            y,
                        )
                    }
                    drawPath(path, accent.copy(alpha = 0.70f - index * 0.15f), style = Stroke(line))
                }
            }

            BeautyFocus.SYSTEM -> {
                // System controls affect the pipeline rather than a facial region.
                drawOval(accent.copy(alpha = 0.34f), faceRect.topLeft, faceRect.size, style = Stroke(line))
            }
        }
    }
}
