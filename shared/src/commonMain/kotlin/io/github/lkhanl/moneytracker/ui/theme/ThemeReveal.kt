package io.github.lkhanl.moneytracker.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import io.github.lkhanl.moneytracker.settings.ThemeMode
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

/**
 * Tema değişimini dokunulan noktayı merkez alan bir daireyle gösterir.
 *
 * Değişimden hemen önce ekranın görüntüsü alınır ve üstte tutulur; altta yeni tema çizilir.
 *  - Açık temaya geçerken eski görüntüde noktadan dışa doğru büyüyen bir delik açılır.
 *  - Koyu temaya geçerken eski görüntü, noktaya doğru küçülerek kapanan bir dairenin içinde kalır.
 *
 * [content], değişimi başlatmak için `(yeni tema, kök koordinatlarda nokta)` alan bir fonksiyon alır.
 */
@Composable
fun ThemeReveal(
    onApply: (ThemeModeChange) -> Unit,
    content: @Composable (switchTheme: (ThemeModeChange) -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val progress = remember { Animatable(0f) }
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var closing by remember { mutableStateOf(false) }

    val switchTheme: (ThemeModeChange) -> Unit = { change ->
        // Bir geçiş sürerken yenisi başlamaz; aksi halde yarım kalmış bir görüntü yakalanır.
        if (snapshot == null) {
            scope.launch {
                snapshot = layer.toImageBitmap()
                origin = change.origin
                closing = change.mode == ThemeMode.DARK
                progress.snapTo(0f)
                onApply(change)
                try {
                    // İki yönde de aynı süre ve aynı eğri: ilk anda belirgin hareket, sona doğru yavaşlama.
                    progress.animateTo(1f, tween(durationMillis = 550, easing = FastOutSlowInEasing))
                } finally {
                    snapshot = null
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)

                val old = snapshot ?: return@drawWithContent
                // Dairenin en büyük hali, noktadan en uzak köşeye ulaşıp ekranın tamamını kaplar.
                val maxRadius = max(
                    max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
                    max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y)),
                )
                val radius = maxRadius * if (closing) 1f - progress.value else progress.value
                val circle = Path().apply { addOval(Rect(center = origin, radius = radius)) }
                // Açılırken eski tema dairenin dışında, kapanırken içinde kalır.
                clipPath(circle, if (closing) ClipOp.Intersect else ClipOp.Difference) { drawImage(old) }
            },
    ) {
        content(switchTheme)
    }
}

/** Yeni tema ve geçişin başlayacağı nokta (kök koordinatlarda). */
data class ThemeModeChange(val mode: ThemeMode, val origin: Offset)
