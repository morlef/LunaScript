package si.f5.luna3419.script.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF007ACC),
    onPrimary = Color.White,
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFCCCCCC),
    surface = Color(0xFF252526),
    onSurface = Color(0xFFCCCCCC),
    surfaceVariant = Color(0xFF383838),
    onSurfaceVariant = Color(0xFFCCCCCC),
    secondaryContainer = Color(0xFF2D2D2D),
    outline = Color(0xFF404040),
    error = Color(0xFFFF0000)
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007ACC),
    onPrimary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF333333),
    surface = Color(0xFFF3F3F3),
    onSurface = Color(0xFF333333),
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = Color(0xFF333333),
    secondaryContainer = Color(0xFFEFEFEF),
    outline = Color(0xFFD4D4D4)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    typography: androidx.compose.material3.Typography = MaterialTheme.typography,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = typography,
        content = content
    )
}
