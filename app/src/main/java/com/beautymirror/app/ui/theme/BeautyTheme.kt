package com.beautymirror.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BmBg = Color(0xFF080A0D)
val BmSurface = Color(0x99171A20)
val BmSurfaceStrong = Color(0xB322252C)
val BmAccent = Color(0xFFFFC4CF)
val BmAccentSoft = Color(0x99FFC4CF)
val BmText = Color(0xFFF6F2F3)
val BmTextMuted = Color(0xA6F6F2F3)
val BmDanger = Color(0xFFFF8A8A)

private val scheme = darkColorScheme(
    primary = BmAccent,
    onPrimary = BmBg,
    secondary = Color(0xFFD8C8FF),
    onSecondary = BmBg,
    background = BmBg,
    surface = Color(0xFF171A20),
    surfaceVariant = Color(0xFF252932),
    onBackground = BmText,
    onSurface = BmText,
    error = BmDanger,
)

@Composable
fun BeautyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography.copy(
            displayLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                fontSize = 40.sp,
                letterSpacing = 1.sp,
                color = BmText,
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = BmText,
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = BmText,
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = BmText,
            ),
        ),
        content = content,
    )
}
