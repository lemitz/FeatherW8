package com.featherw8.player.ui.theme

import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val AmoledBlack = Color(0xFF000000)
internal val SurfaceBlack = Color(0xFF09090B)
internal val CardBlack = Color(0xFF111114)
internal val DefaultAccent = Color(0xFFF23B50)
internal val MiniPlayerContentOverlap = 64.dp
internal val LocalFeatherAccent = staticCompositionLocalOf { DefaultAccent }
internal val FeatherRed: Color
    @Composable get() = LocalFeatherAccent.current
internal val PrimaryText = Color(0xFFF5F5F6)
internal val MutedText = Color(0xFFA3A2A5)
internal val ArtworkPlaceholder = Color(0xFF31191E)
internal data class AccentOption(val id: String, val label: String, val color: Color)
internal val AccentOptions = listOf(
    AccentOption("red", "Crimson", Color(0xFFF23B50)),
    AccentOption("violet", "Violet", Color(0xFFAE80FF)),
    AccentOption("cyan", "Ice blue", Color(0xFF4CC9F0)),
    AccentOption("mint", "Mint", Color(0xFF3DD6A4)),
    AccentOption("amber", "Amber", Color(0xFFFFB84D)),
    AccentOption("rose", "Rose", Color(0xFFFF6FAE))
)
internal fun accentFor(id: String): Color = AccentOptions.firstOrNull { it.id == id }?.color ?: DefaultAccent

internal enum class DeviceProfile { NOTHING, REALME, GENERIC }

internal fun detectDeviceProfile(): DeviceProfile {
    val device = "${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL}".lowercase()
    return when {
        "nothing" in device || "a142" in device || "phone 3a" in device -> DeviceProfile.NOTHING
        "realme" in device || "rmx3853" in device || "gt 6t" in device -> DeviceProfile.REALME
        else -> DeviceProfile.GENERIC
    }
}

@Composable
internal fun FeatherW8Theme(
    profile: DeviceProfile,
    dotFontEnabled: Boolean,
    accentId: String,
    content: @Composable () -> Unit
) {
    val accent = remember(accentId) { accentFor(accentId) }
    val typeface = remember(profile, dotFontEnabled) {
        if (profile == DeviceProfile.NOTHING && dotFontEnabled) Typeface.create("NDot55", Typeface.NORMAL) else Typeface.DEFAULT
    }
    val family = remember(typeface) { FontFamily(typeface) }
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        secondary = accent.copy(alpha = .84f),
        background = AmoledBlack,
        onBackground = PrimaryText,
        surface = SurfaceBlack,
        onSurface = PrimaryText,
        surfaceVariant = CardBlack,
        onSurfaceVariant = MutedText,
        error = Color(0xFFFF5268)
    )
    MaterialTheme(colorScheme = scheme) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalFeatherAccent provides accent,
            androidx.compose.material3.LocalContentColor provides PrimaryText,
            androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontFamily = family),
            content = content
        )
    }
}

@Composable
internal fun navColors() = androidx.compose.material3.NavigationBarItemDefaults.colors(
    selectedIconColor = FeatherRed,
    selectedTextColor = FeatherRed,
    indicatorColor = FeatherRed.copy(alpha = .10f),
    unselectedIconColor = MutedText,
    unselectedTextColor = MutedText
)
