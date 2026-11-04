package io.github.lkhanl.moneytracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.lkhanl.moneytracker.ui.theme.AppTheme
import kotlin.math.abs
import kotlin.math.roundToInt

private val BarHeight = 68.dp
private val DropRadius = 24.dp

/** Damla, çubuğun üst kenarından bu kadar taşar; çubuğun toplam yüksekliğine eklenir. */
internal val NavBarOverhang = 24.dp
private val Overhang = NavBarOverhang

/** Damla ile çubuk arasındaki boşluk; damlanın etrafındaki "halka" bu kadar geniştir. */
private val DropGap = 5.dp

/**
 * Seçili sekmenin ikonu çubuktan yükselen bir damlanın içinde durur. Sekme değişince damla
 * yaylanarak yeni yerine kayar; yol boyunca hızına göre yatayda esner, varınca yerine oturur.
 */
@Composable
fun DropletNavigationBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val tabs = Tab.entries
    val colors = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(Modifier.fillMaxWidth().height(Overhang + BarHeight + bottomInset)) {
        val itemWidthPx = with(density) { maxWidth.toPx() } / tabs.size
        val targetX = itemWidthPx * (selected.ordinal + 0.5f)

        // Damlanın yatay konumu. İlk çizimde animasyonsuz yerine konur.
        val dropX = remember { Animatable(targetX) }
        LaunchedEffect(targetX) {
            dropX.animateTo(targetX, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
        }
        // Hedefe olan uzaklık hareket halindeyken büyür: damlayı esnetmek için kullanılır.
        val stretch = (abs(targetX - dropX.value) / itemWidthPx).coerceIn(0f, 1f)
        // Yay hedefi biraz aşar; kenar sekmelerde damla ekrandan taşmasın diye aşma payı sınırlı.
        val overshoot = with(density) { 10.dp.toPx() }
        val widthPx = itemWidthPx * tabs.size
        val cxDrawn = dropX.value.coerceIn(itemWidthPx / 2 - overshoot, widthPx - itemWidthPx / 2 + overshoot)

        val barColor = colors.surfaceContainerHigh
        val border = extra.cardBorder

        Canvas(Modifier.fillMaxSize()) {
            val barTop = Overhang.toPx()
            val r = DropRadius.toPx() + DropGap.toPx()
            val cy = barTop + 6.dp.toPx()
            val cx = cxDrawn
            // Hareket ederken tümsek genişler ve alçalır; damla çubuğu peşinden sürüklüyor gibi görünür.
            val half = r * (2f + 0.8f * stretch)
            val peak = cy - r * (1f - 0.12f * stretch)

            fun Path.topEdge() {
                moveTo(0f, barTop)
                lineTo(cx - half, barTop)
                cubicTo(cx - half * 0.55f, barTop, cx - r, peak, cx, peak)
                cubicTo(cx + r, peak, cx + half * 0.55f, barTop, cx + half, barTop)
                lineTo(size.width, barTop)
            }
            val top = Path().apply { topEdge() }
            // Gövde, üst kenarla aynı alt yolun devamı olmalı; ayrı bir yol eklenirse tümsek dolmaz.
            val body = Path().apply {
                topEdge()
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            // Yumuşak üst gölge: çubuğu içerikten ayırır.
            translate(top = -2.dp.toPx()) {
                drawPath(body, colors.scrim.copy(alpha = 0.06f))
            }
            drawPath(body, barColor)
            drawPath(top, border, style = Stroke(width = 1.dp.toPx()))
        }

        // Damla.
        val dropScaleX = 1f + 0.18f * stretch
        val dropScaleY = 1f - 0.12f * stretch
        val dropSizePx = with(density) { (DropRadius * 2).toPx() }
        val dropTopPx = with(density) { (Overhang + 6.dp - DropRadius).toPx() }
        Box(
            Modifier
                .offset { IntOffset((cxDrawn - dropSizePx / 2).roundToInt(), dropTopPx.roundToInt()) }
                .size(DropRadius * 2)
                .graphicsLayer {
                    scaleX = dropScaleX
                    scaleY = dropScaleY
                }
                .shadow(8.dp, CircleShape, ambientColor = extra.navIndicator, spotColor = extra.navIndicator)
                .background(extra.navIndicator, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.5f), initialScale = 0.4f) + fadeIn()) togetherWith
                        (scaleOut(targetScale = 0.4f) + fadeOut())
                },
                label = "dropIcon",
            ) { tab ->
                Icon(painterResource(tab.icon), contentDescription = null, tint = extra.onNavIndicator)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = Overhang)
                .height(BarHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selected
                // Seçili sekmenin ikonu damlaya taşındığı için yerindeki kopyası kaybolur.
                val iconAlpha by animateFloatAsState(if (isSelected) 0f else 1f, label = "iconAlpha")
                val labelColor by animateColorAsState(
                    if (isSelected) colors.secondary else colors.onSurfaceVariant,
                    label = "labelColor",
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = isSelected,
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                        ) {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(tab)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Icon(
                        painterResource(tab.icon),
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.alpha(iconAlpha).graphicsLayer {
                            // Kaybolurken yukarı, damlaya doğru kayar.
                            translationY = -(1f - iconAlpha) * 12.dp.toPx()
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = labelColor,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
