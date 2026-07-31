package com.beautymirror.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beautymirror.app.R
import com.beautymirror.app.ui.theme.BmAccent
import com.beautymirror.app.ui.theme.BmSurfaceStrong
import com.beautymirror.app.ui.theme.BmText
import com.beautymirror.app.ui.theme.BmTextMuted

enum class BeautyFocus {
    OVERVIEW,
    EYES,
    UNDER_EYES,
    SKIN,
    LIPS,
    SHAPE,
    SYSTEM,
}

@Composable
fun BeautyFeatureGuide(
    focus: BeautyFocus,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BmSurfaceStrong)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FaceFeatureGraphic(
            focus = focus,
            modifier = Modifier.size(88.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, color = BmText, fontSize = 15.sp)
            Text(description, color = BmTextMuted, fontSize = 11.sp)
            Text(
                text = focusLabel(focus),
                color = BmAccent,
                fontSize = 10.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(BmAccent.copy(alpha = 0.11f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun FaceFeatureGraphic(
    focus: BeautyFocus,
    modifier: Modifier = Modifier,
) {
    val accent = BmAccent
    val muted = BmTextMuted.copy(alpha = 0.46f)
    val softAccent = BmAccent.copy(alpha = 0.20f)
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val face = Rect(w * 0.20f, h * 0.06f, w * 0.80f, h * 0.94f)
            val eyeY = h * 0.39f
            val leftEye = Offset(w * 0.37f, eyeY)
            val rightEye = Offset(w * 0.63f, eyeY)
            val eyeSize = Size(w * 0.16f, h * 0.065f)
            val stroke = Stroke(width = 2.2f)

            if (focus == BeautyFocus.SKIN || focus == BeautyFocus.OVERVIEW) {
                drawOval(softAccent, topLeft = face.topLeft, size = face.size)
            }
            drawOval(muted, topLeft = face.topLeft, size = face.size, style = stroke)

            val eyeColor = if (focus == BeautyFocus.EYES || focus == BeautyFocus.OVERVIEW) accent else muted
            drawOval(
                eyeColor,
                topLeft = Offset(leftEye.x - eyeSize.width / 2f, leftEye.y - eyeSize.height / 2f),
                size = eyeSize,
                style = stroke,
            )
            drawOval(
                eyeColor,
                topLeft = Offset(rightEye.x - eyeSize.width / 2f, rightEye.y - eyeSize.height / 2f),
                size = eyeSize,
                style = stroke,
            )
            if (focus == BeautyFocus.EYES || focus == BeautyFocus.OVERVIEW) {
                drawCircle(accent, radius = w * 0.018f, center = leftEye)
                drawCircle(accent, radius = w * 0.018f, center = rightEye)
                drawLine(accent, Offset(w * 0.30f, h * 0.31f), Offset(w * 0.44f, h * 0.29f), 3f)
                drawLine(accent, Offset(w * 0.56f, h * 0.29f), Offset(w * 0.70f, h * 0.31f), 3f)
            }

            val underColor = if (focus == BeautyFocus.UNDER_EYES || focus == BeautyFocus.OVERVIEW) accent else muted
            drawArc(
                underColor,
                startAngle = 12f,
                sweepAngle = 156f,
                useCenter = false,
                topLeft = Offset(w * 0.28f, h * 0.39f),
                size = Size(w * 0.20f, h * 0.14f),
                style = Stroke(width = if (focus == BeautyFocus.UNDER_EYES) 5f else 2f),
            )
            drawArc(
                underColor,
                startAngle = 12f,
                sweepAngle = 156f,
                useCenter = false,
                topLeft = Offset(w * 0.52f, h * 0.39f),
                size = Size(w * 0.20f, h * 0.14f),
                style = Stroke(width = if (focus == BeautyFocus.UNDER_EYES) 5f else 2f),
            )

            val nose = Path().apply {
                moveTo(w * 0.50f, h * 0.43f)
                lineTo(w * 0.46f, h * 0.63f)
                quadraticBezierTo(w * 0.50f, h * 0.67f, w * 0.54f, h * 0.63f)
            }
            drawPath(nose, if (focus == BeautyFocus.SHAPE) accent else muted, style = stroke)

            val lipsColor = if (focus == BeautyFocus.LIPS || focus == BeautyFocus.OVERVIEW) accent else muted
            val lips = Path().apply {
                moveTo(w * 0.36f, h * 0.75f)
                quadraticBezierTo(w * 0.44f, h * 0.68f, w * 0.50f, h * 0.73f)
                quadraticBezierTo(w * 0.56f, h * 0.68f, w * 0.64f, h * 0.75f)
                quadraticBezierTo(w * 0.50f, h * 0.86f, w * 0.36f, h * 0.75f)
                close()
            }
            if (focus == BeautyFocus.LIPS || focus == BeautyFocus.OVERVIEW) {
                drawPath(lips, softAccent)
            }
            drawPath(lips, lipsColor, style = stroke)

            if (focus == BeautyFocus.SHAPE) {
                drawArc(
                    accent,
                    startAngle = 35f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(w * 0.17f, h * 0.18f),
                    size = Size(w * 0.66f, h * 0.75f),
                    style = Stroke(width = 4f),
                )
            }

            if (focus == BeautyFocus.SYSTEM) {
                drawCircle(accent, radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.52f), style = Stroke(4f))
                drawLine(accent, Offset(w * 0.50f, h * 0.32f), Offset(w * 0.50f, h * 0.40f), 4f)
            }
        }
    }
}

@Composable
private fun focusLabel(focus: BeautyFocus): String = stringResource(
    when (focus) {
        BeautyFocus.OVERVIEW -> R.string.focus_overview
        BeautyFocus.EYES -> R.string.focus_eyes
        BeautyFocus.UNDER_EYES -> R.string.focus_under_eyes
        BeautyFocus.SKIN -> R.string.focus_skin
        BeautyFocus.LIPS -> R.string.focus_lips
        BeautyFocus.SHAPE -> R.string.focus_shape
        BeautyFocus.SYSTEM -> R.string.focus_system
    },
)
