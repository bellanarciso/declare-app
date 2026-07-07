package edu.illinois.cs.cs124.ay2026.project.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary              = Forest,
    onPrimary            = Cream,
    primaryContainer     = Butter,
    onPrimaryContainer   = Forest,
    secondary            = Sage,
    onSecondary          = Forest,
    secondaryContainer   = Sage.copy(alpha = 0.3f),
    onSecondaryContainer = Forest,
    tertiary             = Butter,
    onTertiary           = Forest,
    background           = Cream,
    onBackground         = Deep,
    surface              = Paper,
    onSurface            = Deep,
    surfaceVariant       = Cream,
    onSurfaceVariant     = Forest,
    outline              = Sage,
    outlineVariant       = Sage.copy(alpha = 0.4f),
    error                = Terracotta,
    onError              = Cream
)

private val DarkColors = darkColorScheme(
    primary              = Butter,
    onPrimary            = Deep,
    primaryContainer     = Forest,
    onPrimaryContainer   = Cream,
    secondary            = Sage,
    onSecondary          = Deep,
    secondaryContainer   = Forest,
    onSecondaryContainer = Cream,
    tertiary             = Butter,
    onTertiary           = Deep,
    background           = Deep,
    onBackground         = Cream,
    surface              = DarkSurface,
    onSurface            = Cream,
    surfaceVariant       = Forest,
    onSurfaceVariant     = Cream,
    outline              = Sage.copy(alpha = 0.6f),
    outlineVariant       = Forest,
    error                = Terracotta,
    onError              = Cream
)

@Composable
fun DeclaredTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = DeclaredMaterialTypography,
        shapes      = DeclaredShapes,
        content     = content
    )
}