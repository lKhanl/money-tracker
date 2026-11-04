package io.github.lkhanl.moneytracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.lkhanl.moneytracker.core.Category

/*
 * Palet: mor (primary) + limon yeşili (secondary) + kömür grisi zemin.
 * Duvar kâğıdından türeyen dinamik renkler yerine sabit palet kullanılıyor ki
 * uygulama her cihazda aynı görünsün.
 */

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9B7DFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7B3FF0),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFD4F04A),
    onSecondary = Color(0xFF1A1F00),
    secondaryContainer = Color(0xFF3A4500),
    onSecondaryContainer = Color(0xFFE8FA9E),
    tertiary = Color(0xFFD4F04A),
    onTertiary = Color(0xFF1A1F00),
    tertiaryContainer = Color(0xFF6A45E0),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF150F24),
    onBackground = Color(0xFFF3F0FA),
    surface = Color(0xFF150F24),
    onSurface = Color(0xFFF3F0FA),
    surfaceVariant = Color(0xFF342852),
    onSurfaceVariant = Color(0xFFB3A9CC),
    surfaceContainerLowest = Color(0xFF0F0A1B),
    surfaceContainerLow = Color(0xFF1C1530),
    surfaceContainer = Color(0xFF221A3A),
    surfaceContainerHigh = Color(0xFF2A2046),
    surfaceContainerHighest = Color(0xFF342852),
    outline = Color(0xFF6C5E8C),
    outlineVariant = Color(0xFF3A2E58),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1A16),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6A3FE0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6DDFF),
    onPrimaryContainer = Color(0xFF21005E),
    secondary = Color(0xFF4E6000),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3F79A),
    onSecondaryContainer = Color(0xFF1A1F00),
    tertiary = Color(0xFFD4F04A),
    onTertiary = Color(0xFF1A1F00),
    tertiaryContainer = Color(0xFF6A45E0),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFFF4F2FA),
    onBackground = Color(0xFF1B1A22),
    surface = Color(0xFFF4F2FA),
    onSurface = Color(0xFF1B1A22),
    surfaceVariant = Color(0xFFDCD7EA),
    onSurfaceVariant = Color(0xFF4D4A5A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDEAF5),
    surfaceContainer = Color(0xFFE7E3F1),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFDCD7EA),
    outline = Color(0xFF79758A),
    outlineVariant = Color(0xFFCBC6DA),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

/** Uygulamaya özel, Material'da karşılığı olmayan renkler. */
@Immutable
data class ExtraColors(
    val income: Color,
    /** Ana sayfadaki özet kartı: ön yüz geçişi, arkasındaki koyu katman ve halka. */
    val heroGradient: List<Color>,
    val heroBack: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val heroRingTrack: Color,
    val heroRingArc: Color,
    /** Zeminin sağ üstündeki büyük renk lekesi. */
    val backdropBlob: Color,
    /** Alt menüde seçili sekmenin göstergesi. */
    val navIndicator: Color,
    val onNavIndicator: Color,
    /** Kategori renkleri sabit; OTHER her zaman nötr. */
    val categories: Map<Category, Color>,
    /** Kategori ikonunun, kendi renkli zemini üzerindeki rengi. */
    val onCategory: Color,
    val cardBorder: Color,
)

private val Lime = Color(0xFFD4F04A)

private val DarkExtra = ExtraColors(
    income = Lime,
    heroGradient = listOf(Color(0xFFDDF65A), Color(0xFFC6EA38)),
    heroBack = Color(0xFF9DC21F),
    onHero = Color(0xFF15180A),
    onHeroMuted = Color(0xFF3D4515),
    heroRingTrack = Color(0xFFE9FB9C),
    heroRingArc = Color(0xFF7B3FF0),
    backdropBlob = Color(0xFF5A22C9),
    navIndicator = Lime,
    onNavIndicator = Color(0xFF15180A),
    categories = mapOf(
        Category.FUEL to Color(0xFFC8F03C),
        Category.TRANSFER to Color(0xFFA64DFF),
        Category.MARKET to Color(0xFF5ED8F0),
        Category.SHOPPING to Color(0xFFFFD43B),
        Category.SUBSCRIPTION to Color(0xFF3DDC6E),
        Category.OTHER to Color(0xFF4A3F66),
    ),
    onCategory = Color(0xFF15180A),
    cardBorder = Color(0xFF3A2E58),
)

private val LightExtra = ExtraColors(
    income = Color(0xFF3B7A12),
    heroGradient = listOf(Color(0xFFDDF65A), Color(0xFFC6EA38)),
    heroBack = Color(0xFF9DC21F),
    onHero = Color(0xFF15180A),
    onHeroMuted = Color(0xFF3D4515),
    heroRingTrack = Color(0xFFE9FB9C),
    heroRingArc = Color(0xFF7B3FF0),
    backdropBlob = Color(0xFFD9CCFF),
    navIndicator = Lime,
    onNavIndicator = Color(0xFF15180A),
    categories = mapOf(
        Category.FUEL to Color(0xFFB5DC22),
        Category.TRANSFER to Color(0xFFA64DFF),
        Category.MARKET to Color(0xFF3CC3E0),
        Category.SHOPPING to Color(0xFFFFC917),
        Category.SUBSCRIPTION to Color(0xFF2CC65B),
        Category.OTHER to Color(0xFFB9B2CC),
    ),
    onCategory = Color(0xFF15180A),
    cardBorder = Color(0xFFDAD5E6),
)

private val LocalExtraColors = staticCompositionLocalOf { DarkExtra }

object AppTheme {
    val extra: ExtraColors
        @Composable @ReadOnlyComposable get() = LocalExtraColors.current
}

fun Category.color(extra: ExtraColors): Color = extra.categories.getValue(this)

@Composable
fun MoneyTrackerTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors: ColorScheme = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalExtraColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
