package com.example.dormcheck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE5FB),
    onPrimaryContainer = TextStrong,
    secondary = Accent3,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E2FB),
    onSecondaryContainer = TextStrong,
    tertiary = InfoCyan,
    onTertiary = Color.White,
    background = GlassBg2,
    onBackground = TextBody,
    surface = Color.White,
    onSurface = TextBody,
    surfaceVariant = Color(0xFFE6ECF7),
    onSurfaceVariant = TextSub,
    outline = Color(0xFF97A3B1),
    outlineVariant = Color(0xFFD5DDE6),
    error = DangerRed,
    onError = Color.White,
    errorContainer = Color(0xFFFDECE9),
    onErrorContainer = DangerRed
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF0B1730),
    primaryContainer = Color(0xFF23304A),
    onPrimaryContainer = TextStrongDark,
    secondary = Accent3Dark,
    onSecondary = Color(0xFF12082B),
    secondaryContainer = Color(0xFF2A2444),
    onSecondaryContainer = TextStrongDark,
    tertiary = InfoCyanDark,
    onTertiary = Color(0xFF04222B),
    background = GlassBg2Dark,
    onBackground = TextBodyDark,
    surface = Color(0xFF151D2C),
    onSurface = TextBodyDark,
    surfaceVariant = Color(0xFF243044),
    onSurfaceVariant = TextSubDark,
    outline = Color(0xFF7A8694),
    outlineVariant = Color(0xFF3B444E),
    error = DangerRedDark,
    onError = Color(0xFF3B0A05),
    errorContainer = Color(0xFF4A1512),
    onErrorContainer = DangerRedDark
)

/**
 * 玻璃层令牌：卡片、内嵌块、语义色的透明度都在这里统一，
 * 组件里不再写死颜色，换主题只动这一个文件。
 */
@Immutable
data class GlassPalette(
    val glassTop: Color,
    val glassBottom: Color,
    val glassLine: Color,
    val softFill: Color,
    val softLine: Color,
    val text: Color,
    val textStrong: Color,
    val textSub: Color,
    val textMuted: Color,
    val accent: Color,
    val accent2: Color,
    val accent3: Color,
    val ok: Color,
    val danger: Color,
    val warn: Color,
    val info: Color,
    val okTint: Color,
    val okLine: Color,
    val dangerTint: Color,
    val dangerLine: Color,
    val warnTint: Color,
    val warnLine: Color,
    val isDark: Boolean
)

val LightGlassPalette = GlassPalette(
    glassTop = Color.White.copy(alpha = 0.80f),
    glassBottom = Color.White.copy(alpha = 0.52f),
    glassLine = Color.White.copy(alpha = 0.75f),
    softFill = Color.White.copy(alpha = 0.40f),
    softLine = Color.White.copy(alpha = 0.70f),
    text = TextBody,
    textStrong = TextStrong,
    textSub = TextSub,
    textMuted = TextMuted,
    accent = Accent,
    accent2 = Accent2,
    accent3 = Accent3,
    ok = OkGreen,
    danger = DangerRed,
    warn = WarnAmber,
    info = InfoCyan,
    okTint = OkGreen.copy(alpha = 0.10f),
    okLine = OkGreen.copy(alpha = 0.34f),
    dangerTint = DangerRed.copy(alpha = 0.09f),
    dangerLine = DangerRed.copy(alpha = 0.30f),
    warnTint = Color(0xFFFFBE50).copy(alpha = 0.14f),
    warnLine = Color(0xFFFFBE50).copy(alpha = 0.45f),
    isDark = false
)

val DarkGlassPalette = GlassPalette(
    glassTop = Color.White.copy(alpha = 0.11f),
    glassBottom = Color.White.copy(alpha = 0.05f),
    glassLine = Color.White.copy(alpha = 0.16f),
    softFill = Color.White.copy(alpha = 0.07f),
    softLine = Color.White.copy(alpha = 0.14f),
    text = TextBodyDark,
    textStrong = TextStrongDark,
    textSub = TextSubDark,
    textMuted = TextMutedDark,
    accent = AccentDark,
    accent2 = Accent2Dark,
    accent3 = Accent3Dark,
    ok = OkGreenDark,
    danger = DangerRedDark,
    warn = WarnAmberDark,
    info = InfoCyanDark,
    okTint = OkGreenDark.copy(alpha = 0.16f),
    okLine = OkGreenDark.copy(alpha = 0.40f),
    dangerTint = DangerRedDark.copy(alpha = 0.15f),
    dangerLine = DangerRedDark.copy(alpha = 0.38f),
    warnTint = WarnAmberDark.copy(alpha = 0.14f),
    warnLine = WarnAmberDark.copy(alpha = 0.40f),
    isDark = true
)

val LocalGlassPalette = staticCompositionLocalOf { LightGlassPalette }

@Composable
fun DormCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalGlassPalette provides if (darkTheme) DarkGlassPalette else LightGlassPalette
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}
