package com.example.earview.theme

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

private val DarkColorScheme = darkColorScheme(
  primary = Mint,
  onPrimary = Color(0xFF07382A),
  primaryContainer = Color(0xFF204E3D),
  onPrimaryContainer = Color(0xFFD3F2E5),
  secondary = Color(0xFFB7CEC2),
  background = CanvasDark,
  onBackground = Color(0xFFE4EEE8),
  surface = Color(0xFF192720),
  onSurface = Color(0xFFE4EEE8),
  surfaceVariant = Color(0xFF293B32),
  onSurfaceVariant = Color(0xFFB8C9BF),
  outline = Color(0xFF83998C),
  outlineVariant = Color(0xFF40594B),
  error = Color(0xFFFFB3BA),
  onError = Color(0xFF650D20),
  errorContainer = Color(0xFF842336),
  onErrorContainer = Color(0xFFFFD9DC),
)

private val LightColorScheme =
  lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEEE4),
    onPrimaryContainer = Color(0xFF174E3B),
    secondary = Color(0xFF476454),
    background = CanvasLight,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAF0EC),
    onSurfaceVariant = MutedInk,
    outline = Color(0xFF72867A),
    outlineVariant = Color(0xFFD8E2DB),
    error = StatusDetected,
    onError = Color.White,
    errorContainer = Color(0xFFFFE7E9),
    onErrorContainer = Color(0xFF7D1F2D),
  )

@Composable
fun EarViewTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Stable colors by default so wallpaper never changes permission-status meaning.
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
