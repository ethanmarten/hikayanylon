package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GoldPrimary,
    onPrimary = NavyDeep,
    primaryContainer = NavySurface,
    onPrimaryContainer = GoldAccent,
    secondary = GoldAccent,
    onSecondary = NavyDeep,
    background = NavyDeep,
    onBackground = TextOnNavy,
    surface = NavySurface,
    onSurface = TextOnNavy,
    surfaceVariant = NavyLight,
    onSurfaceVariant = TextOnNavy,
    outline = GoldPrimary
  )

private val LightColorScheme = DarkColorScheme // Always use the gorgeous Navy and Gold luxury theme!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to true for the deep navy luxury look
  dynamicColor: Boolean = false, // Force brand colors instead of system dynamic colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme // Always enforce our custom luxurious brand theme!

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
