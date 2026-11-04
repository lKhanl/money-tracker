package io.github.lkhanl.moneytracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lkhanl.moneytracker.resources.Res
import kotlinx.coroutines.flow.first

private val SplashBackground = Color(0xFF07050D)
private val Lime = Color(0xFFCCFA1E)
private val Violet = Color(0xFF9B3DFF)
private val BarTrack = Color(0xFF4A2D80)

/** Açılış ekranı en az bu kadar görünür; çubuk bu sürede dolar. */
private const val MinDurationMs = 1000

/**
 * Uygulama açılışında bir kez gösterilir: cam kart ve madeni para illüstrasyonu, bir alıntı,
 * logo ve dolan çubuk. En az [MinDurationMs] görünür; veriler henüz hazır değilse ([ready])
 * hazır olana kadar bekler, sonra [onFinished] çağrılır. Tema seçiminden bağımsız olarak hep koyudur.
 */
@Composable
fun SplashScreen(ready: Boolean, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val isReady by rememberUpdatedState(ready)
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(MinDurationMs, easing = FastOutSlowInEasing))
        snapshotFlow { isReady }.first { it }
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SplashBackground)
            .drawBehind { drawBackdrop() },
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.7f))
            Canvas(Modifier.width(300.dp).height(230.dp)) { drawIllustration() }
            Spacer(Modifier.height(36.dp))
            Quote(Modifier.fillMaxWidth().padding(horizontal = 48.dp))
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Canvas(Modifier.size(40.dp)) { drawLogo() }
                Text(
                    "Money Tracker",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(28.dp))
            Canvas(
                Modifier
                    .width(148.dp)
                    .height(7.dp)
                    .semantics { contentDescription = "Yükleniyor" },
            ) {
                val h = size.height
                drawLine(BarTrack, Offset(h / 2, h / 2), Offset(size.width - h / 2, h / 2), h, StrokeCap.Round)
                val end = h / 2 + (size.width - h) * progress.value
                drawLine(Lime, Offset(h / 2, h / 2), Offset(end, h / 2), h, StrokeCap.Round)
            }
            Spacer(Modifier.weight(0.75f))
        }
    }
}

@Composable
private fun Quote(modifier: Modifier) {
    Column(modifier) {
        Text("“", Modifier.height(40.dp), color = Lime, fontSize = 48.sp, fontWeight = FontWeight.Black, lineHeight = 48.sp)
        Text(
            buildAnnotatedString {
                append("Paranı yönetmek,\n")
                withStyle(SpanStyle(color = Lime, fontWeight = FontWeight.Bold)) { append("hayatını yönetmektir.") }
            },
            color = Color.White,
            fontSize = 27.sp,
            lineHeight = 34.sp,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "—  WARREN BUFFETT",
            color = Color(0xFF8E8A99),
            fontSize = 12.sp,
            letterSpacing = 3.sp,
        )
    }
}

/** Halka logosu: limon yeşili halka, tepeden saat 3 yönüne mor dilim. */
private fun DrawScope.drawLogo() {
    val stroke = size.minDimension * 0.2f
    val radius = (size.minDimension - stroke) / 2
    drawCircle(Lime, radius, style = Stroke(stroke))
    drawArc(
        Violet,
        startAngle = -90f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
}

/**
 * Arkaya yaslanmış iki yarı saydam mor kart; öndekinde çip ve parlayan limon yeşili grafik,
 * sağ altta bir deste madeni para. Koordinatlar tuvalin oranı olarak verilir.
 */
private fun DrawScope.drawIllustration() {
    val w = size.width
    val h = size.height
    fun p(x: Float, y: Float) = Offset(w * x, h * y)
    val corner = 18.dp.toPx()

    // Zeminde yansıyan ışık.
    drawOval(
        Brush.radialGradient(
            listOf(Lime.copy(alpha = 0.22f), Violet.copy(alpha = 0.12f), Color.Transparent),
            center = p(0.5f, 0.92f),
            radius = w * 0.45f,
        ),
        topLeft = p(0.05f, 0.80f),
        size = Size(w * 0.9f, h * 0.24f),
    )

    // Arka kart.
    val back = roundedPolygon(listOf(p(0.08f, 0.26f), p(0.76f, 0.05f), p(0.80f, 0.60f), p(0.03f, 0.78f)), corner)
    drawPath(back, Brush.linearGradient(listOf(Color(0x66B48CFF), Color(0x403A12A0)), p(0f, 0f), p(1f, 1f)))
    drawPath(back, Color(0x99C9AEFF), style = Stroke(1.5.dp.toPx()))

    // Ön kart.
    val front = roundedPolygon(listOf(p(0.17f, 0.31f), p(0.92f, 0.15f), p(0.90f, 0.72f), p(0.10f, 0.88f)), corner)
    drawPath(front, Brush.linearGradient(listOf(Color(0x80B48CFF), Color(0x596A2BE8)), p(0f, 0f), p(1f, 1f)))

    // Grafik: kartın içine kırpılmış dalga ve altındaki limon yeşili dolgu.
    val wave = Path().apply {
        moveTo(w * 0.14f, h * 0.80f)
        cubicTo(w * 0.26f, h * 0.80f, w * 0.30f, h * 0.60f, w * 0.40f, h * 0.60f)
        cubicTo(w * 0.50f, h * 0.60f, w * 0.52f, h * 0.68f, w * 0.60f, h * 0.64f)
        cubicTo(w * 0.70f, h * 0.58f, w * 0.72f, h * 0.38f, w * 0.91f, h * 0.36f)
    }
    clipPath(front) {
        val fill = Path().apply {
            addPath(wave)
            lineTo(w * 0.95f, h * 0.95f)
            lineTo(w * 0.10f, h * 0.95f)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(Lime.copy(alpha = 0.55f), Lime.copy(alpha = 0.10f)), h * 0.36f, h * 0.9f))
        for (i in 3 downTo 1) drawPath(wave, Lime.copy(alpha = 0.12f), style = Stroke(i * 5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(wave, Color(0xFFE4FF6A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
    drawPath(front, Brush.linearGradient(listOf(Color(0xCCFFFFFF), Color(0x40FFFFFF)), p(0f, 0f), p(1f, 1f)), style = Stroke(1.5.dp.toPx()))

    // Çip.
    rotate(-12f, p(0.27f, 0.43f)) {
        val tl = p(0.22f, 0.38f)
        val chip = Size(w * 0.10f, h * 0.11f)
        drawRoundRect(Color(0xCCC9AEFF), tl, chip, CornerRadius(5.dp.toPx()))
        val line = Color(0x998F5BFF)
        drawLine(line, Offset(tl.x, tl.y + chip.height / 2), Offset(tl.x + chip.width, tl.y + chip.height / 2), 1.dp.toPx())
        drawLine(line, Offset(tl.x + chip.width / 2, tl.y), Offset(tl.x + chip.width / 2, tl.y + chip.height), 1.dp.toPx())
    }

    // Madeni para destesi: alttan üste, her biri biraz kaymış.
    val coinW = w * 0.30f
    val coinH = h * 0.12f
    val thick = h * 0.045f
    listOf(p(0.79f, 0.84f), p(0.80f, 0.79f), p(0.82f, 0.74f), p(0.84f, 0.68f)).forEach { c ->
        val tl = Offset(c.x - coinW / 2, c.y - coinH / 2)
        val side = Brush.horizontalGradient(
            listOf(Lime.copy(alpha = 0.85f), Color(0xFF6E5A96), Color(0xFF3A2B5C)),
            tl.x,
            tl.x + coinW,
        )
        drawOval(side, tl + Offset(0f, thick), Size(coinW, coinH))
        drawRect(side, tl + Offset(0f, coinH / 2), Size(coinW, thick))
        drawOval(
            Brush.linearGradient(listOf(Color(0xFFD9CCF2), Color(0xFF7A6A9E)), tl, tl + Offset(coinW, coinH)),
            tl,
            Size(coinW, coinH),
        )
        drawOval(
            Color(0x66FFFFFF),
            tl + Offset(coinW * 0.2f, coinH * 0.2f),
            Size(coinW * 0.6f, coinH * 0.6f),
            style = Stroke(1.dp.toPx()),
        )
    }
}

/** Sağ üstte üst üste binen iki, sol altta bir büyük mor daire; kenarları parlak, içleri koyu. */
private fun DrawScope.drawBackdrop() {
    val w = size.width
    val h = size.height
    glowingDisc(Offset(w * 1.05f, h * 0.02f), w * 0.62f)
    glowingDisc(Offset(w * 1.35f, h * 0.36f), w * 0.72f)
    glowingDisc(Offset(-w * 0.15f, h * 1.15f), w * 0.78f)
}

private fun DrawScope.glowingDisc(center: Offset, radius: Float) {
    // Dış hale: kenardan dışarı doğru sönen ışık.
    val haloRadius = radius * 1.18f
    drawCircle(
        Brush.radialGradient(
            0f to Color.Transparent,
            radius / haloRadius to Violet.copy(alpha = 0.22f),
            1f to Color.Transparent,
            center = center,
            radius = haloRadius,
        ),
        haloRadius,
        center,
    )
    // Disk: içi koyu, kenara doğru parlaklaşan mor.
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFF07040E),
            0.72f to Color(0xFF120726),
            0.92f to Color(0xFF2E0F78),
            1f to Color(0xFF6A2BE8),
            center = center,
            radius = radius,
        ),
        radius,
        center,
    )
}
