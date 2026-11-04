package io.github.lkhanl.moneytracker.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

/**
 * Ekranın üstünde bir panel açıkken arka planı bulanıklaştırmak için ortak sayaç.
 * Panel ayrı pencerede çizildiği için bulanıklık yalnızca arkadaki ekrana uygulanır.
 * Bulanıklık Android 12+ cihazlarda görünür; daha eskilerde etkisizdir.
 */
val LocalBackdropBlur = staticCompositionLocalOf<MutableIntState> { mutableIntStateOf(0) }

@Composable
fun rememberBackdropBlur(): MutableIntState = remember { mutableIntStateOf(0) }

/** Bu bileşen ekranda kaldıkça arka plan bulanık olur. */
@Composable
fun BlurBackdropWhileShown() {
    val counter = LocalBackdropBlur.current
    DisposableEffect(counter) {
        counter.intValue++
        onDispose { counter.intValue-- }
    }
}

@Composable
fun Modifier.backdropBlur(counter: MutableIntState): Modifier {
    val on = counter.intValue > 0
    // Açılırken yumuşakça gelir, kapanırken beklemeden kalkar.
    val radius by animateDpAsState(
        if (on) 8.dp else 0.dp,
        if (on) tween(250) else snap(),
        label = "backdropBlur",
    )
    return if (radius > 0.dp) blur(radius) else this
}
