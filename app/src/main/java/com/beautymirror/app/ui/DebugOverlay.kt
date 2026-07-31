package com.beautymirror.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beautymirror.app.BuildConfig
import com.beautymirror.app.masks.FaceMaskGeometry
import com.beautymirror.app.rendering.FrameTimingCollector
import com.beautymirror.app.tracking.FaceTrackingResult

@Composable
fun DebugOverlay(
    tracking: FaceTrackingResult,
    timing: FrameTimingCollector.Snapshot?,
    enabled: Boolean,
    topPanelOffset: Dp = 12.dp,
) {
    if (!BuildConfig.DEBUG_OVERLAY_AVAILABLE || !enabled) return
    Box(Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pts = tracking.displayLandmarks
            if (pts.isNotEmpty()) {
                for (p in pts) {
                    drawCircle(Color(0x889EB6C8), radius = 2f, center = Offset(p.x * w, p.y * h))
                }
                fun strokeIndex(indices: IntArray, color: Color) {
                    val path = Path()
                    var first = true
                    for (i in indices) {
                        val p = pts.getOrNull(i) ?: continue
                        val o = Offset(p.x * w, p.y * h)
                        if (first) {
                            path.moveTo(o.x, o.y)
                            first = false
                        } else path.lineTo(o.x, o.y)
                    }
                    path.close()
                    drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                }
                strokeIndex(FaceMaskGeometry.FACE_OVAL, Color(0xAA9EB6C8))
                strokeIndex(FaceMaskGeometry.LEFT_EYE, Color(0xAA7FDBFF))
                strokeIndex(FaceMaskGeometry.RIGHT_EYE, Color(0xAA7FDBFF))
                val faceW = tracking.bounds.width.coerceAtLeast(0.15f)
                val leftUE = FaceMaskGeometry.underEyePolygon(pts, true, faceWidthNorm = faceW)
                val rightUE = FaceMaskGeometry.underEyePolygon(pts, false, faceWidthNorm = faceW)
                fun strokePoly(poly: List<com.beautymirror.app.tracking.LandmarkPoint>, color: Color) {
                    if (poly.size < 2) return
                    val path = Path()
                    path.moveTo(poly[0].x * w, poly[0].y * h)
                    for (i in 1 until poly.size) path.lineTo(poly[i].x * w, poly[i].y * h)
                    path.close()
                    drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                }
                strokePoly(leftUE, Color(0xAAFF8A80))
                strokePoly(rightUE, Color(0xAAFF8A80))
                val b = tracking.bounds
                drawRect(
                    color = Color(0x66FFFFFF),
                    topLeft = Offset(b.left * w, b.top * h),
                    size = androidx.compose.ui.geometry.Size(b.width * w, b.height * h),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = topPanelOffset, start = 12.dp, end = 12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        ) {
            DebugPanel(
                modifier = Modifier.fillMaxWidth(),
                lines = listOf(
                    "state=${tracking.state} conf=${"%.2f".format(tracking.trackingConfidence)}",
                    "opacity=${"%.2f".format(tracking.effectOpacity)} luma=${"%.2f".format(tracking.faceLuminance)}",
                    "pose yaw=${"%.1f".format(tracking.headYaw)} pitch=${"%.1f".format(tracking.headPitch)} roll=${"%.1f".format(tracking.headRoll)}",
                ),
            )
            if (timing != null) {
                DebugPanel(
                    modifier = Modifier.fillMaxWidth(),
                    lines = buildList {
                        add("cam=${"%.1f".format(timing.cameraFps)}fps analysis=${"%.1f".format(timing.analysisFps)}fps")
                        add(
                            "render=${"%.1f".format(timing.gpuFrameMs)}ms " +
                                "p95=${"%.1f".format(timing.p95FrameMs)}ms",
                        )
                        add(
                            "slow=${"%.0f".format(timing.slowFrameRatio * 100.0)}% " +
                                "dropped=${timing.droppedFrames}",
                        )
                        timing.passMs.entries.take(5).forEach { (name, ms) ->
                            add("$name=${"%.1f".format(ms)}ms")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DebugPanel(
    lines: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x88000000))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        lines.forEach { line ->
            Text(line, color = Color.White, fontSize = 10.sp)
        }
    }
}
