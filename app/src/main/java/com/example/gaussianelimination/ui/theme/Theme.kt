package com.example.gaussianelimination.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define the primary color (Indigo/Deep Blue) for a professional, academic look
val PrimarySeed = Color(0xFF3F51B5) // Deep Indigo

// Generated Color Schemes using the primary seed color
private val LightColorScheme = lightColorScheme(
    primary = PrimarySeed, // Indigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6C6FF), // Light container
    onPrimaryContainer = Color(0xFF000C62),

    secondary = Color(0xFF5A5C7E), // Gray/Blue for steps
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0FF),
    onSecondaryContainer = Color(0xFF171B39),

    tertiary = Color(0xFF7A5262), // Accent for b-column/results
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E4),

    background = Color(0xFFFEF7FF),
    surface = Color(0xFFFEF7FF),
    surfaceVariant = Color(0xFFE4E1EC), // Used for the final solution card
    outline = Color(0xFF777777), // Separator lines (matrix | b)
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFADB7FF), // Brighter primary for dark mode
    onPrimary = Color(0xFF001550),
    primaryContainer = Color(0xFF243B8B),

    secondary = Color(0xFFC4C4E7),
    onSecondary = Color(0xFF2C2F4D),
    secondaryContainer = Color(0xFF434665),

    tertiary = Color(0xFFE9B9C9),
    onTertiary = Color(0xFF462635),
    tertiaryContainer = Color(0xFF603C4B),

    background = Color(0xFF1B1B1F),
    surface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF909090),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A)
)

@Composable
fun GaussianEliminationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}