package com.sumit.muzixx.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.sumit.muzixx.viewmodel.MusicViewModel

@Composable
fun MuzixXTheme(
    viewModel: MusicViewModel,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()

    val isMatchSystem = if (viewModel.isSettingsInitialized()) {
        viewModel.settings.appTheme == "Match System"
    } else {
        true
    }

    val dynamicColorScheme = when {
        isMatchSystem && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isSystemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val activeAccent = if (viewModel.isSettingsInitialized()) {
                when (viewModel.settings.appTheme) {
                    "Electric Blue / Cyan" -> ElectricCyan
                    "Lime Green"          -> LimeGreen
                    "Vibrant Yellow"       -> VibrantYellow
                    "Neon Pink / Magenta"  -> NeonPink
                    "Bright Orange"        -> BrightOrange
                    "Neon Red"             -> NeonRed
                    else                   -> NeonRed
                }
            } else {
                NeonRed
            }

            if (isSystemDark) {
                darkColorScheme(
                    primary = activeAccent,
                    onPrimary = DeepBlack,
                    background = DeepBlack,
                    onBackground = Color.White,
                    surface = DarkGray,
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF161616),
                    onSurfaceVariant = LightGray,
                    secondary = activeAccent,
                    tertiary = activeAccent,
                    scrim = Color.Black.copy(alpha = 0.72f)
                )
            } else {
                lightColorScheme(
                    primary = activeAccent,
                    onPrimary = Color.White,
                    background = LightBack,
                    onBackground = Color.Black,
                    surface = Color.White,
                    onSurface = Color.Black,
                    surfaceVariant = Color(0xFFE5E5E5),
                    onSurfaceVariant = Color(0xFF424242),
                    secondary = activeAccent,
                    tertiary = activeAccent,
                    scrim = Color.Black.copy(alpha = 0.4f)
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        content = content
    )
}