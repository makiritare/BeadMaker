package com.example.beadmaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.beadmaker.ui.screens.BeadEditorScreen

private val BeadMakerPastelScheme = lightColorScheme(
    primary = Color(0xFFA56B5A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF6D7C8),
    onPrimaryContainer = Color(0xFF3A2119),
    secondary = Color(0xFF8C7A63),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECE1D2),
    onSecondaryContainer = Color(0xFF2F281F),
    tertiary = Color(0xFFB88973),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF4DED3),
    onTertiaryContainer = Color(0xFF3A241A),
    background = Color(0xFFFAF5EE),
    onBackground = Color(0xFF2F2723),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF2F2723),
    surfaceVariant = Color(0xFFF1E6D8),
    onSurfaceVariant = Color(0xFF5C5148),
    outline = Color(0xFFB5A79A),
    outlineVariant = Color(0xFFD8CCBE),
    scrim = Color(0xFF000000)
)

private val TokyoNightMoonScheme = darkColorScheme(
    primary = Color(0xFF82AAFF),
    onPrimary = Color(0xFF1E2030),
    primaryContainer = Color(0xFF3B4261),
    onPrimaryContainer = Color(0xFFC8D3F5),
    secondary = Color(0xFFC099FF),
    onSecondary = Color(0xFF222436),
    secondaryContainer = Color(0xFF414868),
    onSecondaryContainer = Color(0xFFC8D3F5),
    tertiary = Color(0xFF86E1FC),
    onTertiary = Color(0xFF1B1D2B),
    tertiaryContainer = Color(0xFF2D3F50),
    onTertiaryContainer = Color(0xFFC8D3F5),
    background = Color(0xFF222436),
    onBackground = Color(0xFFC8D3F5),
    surface = Color(0xFF1E2030),
    onSurface = Color(0xFFC8D3F5),
    surfaceVariant = Color(0xFF2F334D),
    onSurfaceVariant = Color(0xFFA9B1D6),
    outline = Color(0xFF444B6A),
    outlineVariant = Color(0xFF2F334D),
    error = Color(0xFFFF757F),
    onError = Color(0xFF3B4261),
    scrim = Color(0xFF000000)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BeadMakerApp()
        }
    }
}

@Composable
private fun BeadMakerApp(
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val colorScheme = if (darkTheme) {
        TokyoNightMoonScheme
    } else {
        BeadMakerPastelScheme
    }

    MaterialTheme(
        colorScheme = colorScheme
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            BeadEditorScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BeadMakerPreview() {
    BeadMakerApp()
}
