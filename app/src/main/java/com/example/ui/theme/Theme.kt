package com.example.ui.theme

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

private val ElegantDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),          // Light lavender
    onPrimary = Color(0xFF381E72),        // Deep violet
    primaryContainer = Color(0xFF49454F), // Medium charcoal
    onPrimaryContainer = Color(0xFFE8DEF8),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF2B2930), // Dark secondary surface
    onSecondaryContainer = Color(0xFFE6E1E5),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    background = Color(0xFF1C1B1F),       // Base dark grey
    onBackground = Color(0xFFE6E1E5),     // Off-white text
    surface = Color(0xFF1C1B1F),          // Standard surface
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2B2930),   // Slightly lighter card/surface
    onSurfaceVariant = Color(0xFFCAC4D0),  // Muted light grey text
    outline = Color(0xFF49454F),          // Dark outline
    outlineVariant = Color(0xFF938F99),   // Lighter outline
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

private val ElegantLightColorScheme = ElegantDarkColorScheme // Maintain Elegant Dark as the primary experience

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to true for the Elegant Dark experience
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our crafted palette
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) ElegantDarkColorScheme else ElegantLightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
