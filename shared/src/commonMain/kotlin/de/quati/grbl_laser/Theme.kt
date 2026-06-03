package de.quati.grbl_laser

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.letsPlot.intern.Feature
import org.jetbrains.letsPlot.themes.flavorHighContrastDark
import org.jetbrains.letsPlot.themes.themeBW
import org.jetbrains.letsPlot.themes.themeClassic
import org.jetbrains.letsPlot.themes.themeGrey
import org.jetbrains.letsPlot.themes.themeLight
import org.jetbrains.letsPlot.themes.themeMinimal
import org.jetbrains.letsPlot.themes.themeMinimal2
import org.jetbrains.letsPlot.themes.themeNone
import org.jetbrains.letsPlot.themes.themeVoid


internal val AppColorScheme = lightColorScheme(
    primary = Color(0xFF1F5E5A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F0ED),
    onPrimaryContainer = Color(0xFF0B2422),
    secondary = Color(0xFF526461),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E7E3),
    onSecondaryContainer = Color(0xFF10201E),
    tertiary = Color(0xFF556179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE5FF),
    onTertiaryContainer = Color(0xFF111B33),
    background = Color(0xFFF6F8F7),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDDE5E2),
    onSurfaceVariant = Color(0xFF414947),
    outline = Color(0xFF6F7976),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

@Composable
fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
)

enum class LetsPlotTheme(private val themeGen: () -> Feature) {
    MINIMAL_2(::themeMinimal2),
    BW(::themeBW),
    GREY(::themeGrey),
    CLASSIC(::themeClassic),
    LIGHT(::themeLight),
    MINIMAL(::themeMinimal),
    VOID(::themeVoid),
    NONE(::themeNone);

    fun generate(isDark: Boolean) = if (isDark)
        themeGen() + flavorHighContrastDark()
    else
        themeGen()
}
