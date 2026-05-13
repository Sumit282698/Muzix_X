package com.sumit.muzixx.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val MuzixColorScheme = darkColorScheme(
    primary = NeonRed,
    background = DeepBlack,
    surface = DarkGray,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MuzixXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MuzixColorScheme,
        typography = Typography,
        content = content
    )
}