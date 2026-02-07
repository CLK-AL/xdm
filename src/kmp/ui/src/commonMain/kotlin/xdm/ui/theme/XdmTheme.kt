package xdm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * XDM theme using Material3.
 *
 * Conversion from Java XDMLookAndFeel.java / XDMTheme.java:
 * - Java: Custom Swing LookAndFeel with hardcoded colors (ColorResource.java)
 *         Dark theme with #1E1E2E background, #313145 panels, blue accents
 * - C#:   WPF uses ResourceDictionary + system theme detection
 *         GTK uses CSS theming via Adwaita
 * - KMP:  Material3 dynamic color with custom XDM palette
 */

private val XdmDarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4A9EFF),       // XDM blue
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF003366),
    secondary = androidx.compose.ui.graphics.Color(0xFF4CAF50),      // Success green
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    background = androidx.compose.ui.graphics.Color(0xFF1E1E2E),     // From Java ColorResource
    onBackground = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    surface = androidx.compose.ui.graphics.Color(0xFF2B2B3D),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF313145),  // From Java panel color
    error = androidx.compose.ui.graphics.Color(0xFFFF5252),
)

private val XdmLightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1976D2),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFBBDEFB),
    secondary = androidx.compose.ui.graphics.Color(0xFF388E3C),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    background = androidx.compose.ui.graphics.Color(0xFFFAFAFA),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1C1C),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1C1C),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF0F0F0),
    error = androidx.compose.ui.graphics.Color(0xFFD32F2F),
)

@Composable
fun XdmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) XdmDarkColors else XdmLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
