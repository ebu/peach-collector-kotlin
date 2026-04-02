package ch.ebu.peachdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PeachPrimary,
    onPrimary = PeachSurface,
    secondary = PeachAccent,
    background = PeachBackground,
    surface = PeachSurface,
    onSurface = PeachOnSurface,
)

@Composable
fun PeachDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
