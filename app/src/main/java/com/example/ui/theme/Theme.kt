package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudPulseDarkScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = TextPrimary,
    primaryContainer = DeepVioletContainer,
    onPrimaryContainer = SoftViolet,
    secondary = NeonTeal,
    onSecondary = ObsidianBg,
    secondaryContainer = DeepTealContainer,
    onSecondaryContainer = NeonTeal,
    tertiary = AmberWarning,
    onTertiary = ObsidianBg,
    tertiaryContainer = DeepAmberContainer,
    onTertiaryContainer = AmberWarning,
    error = CoralDanger,
    onError = ObsidianBg,
    errorContainer = DeepCoralContainer,
    onErrorContainer = CoralDanger,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedCardBg,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

val StudPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudPulseDarkScheme,
        typography = Typography,
        shapes = StudPulseShapes,
        content = content
    )
}
