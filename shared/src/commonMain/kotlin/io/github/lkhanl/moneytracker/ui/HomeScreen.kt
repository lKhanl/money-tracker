package io.github.lkhanl.moneytracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.lkhanl.moneytracker.resources.Res
import io.github.lkhanl.moneytracker.resources.ic_arrow_forward
import io.github.lkhanl.moneytracker.core.Category
import io.github.lkhanl.moneytracker.core.MonthlySummary
import io.github.lkhanl.moneytracker.ui.theme.AppTheme
import io.github.lkhanl.moneytracker.ui.theme.color
import io.github.lkhanl.moneytracker.core.Money
import io.github.lkhanl.moneytracker.core.sumOf
import io.github.lkhanl.moneytracker.data.today
import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearMonth
import kotlin.math.roundToInt
import kotlinx.datetime.YearMonth

private const val RECENT_COUNT = 5

@Composable
fun HomeScreen(
    state: TransactionsUiState,
    onSeeAll: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + NavBarOverhang),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { MonthSelector(state, onSelectMonth) }
        item { HeroCard(state.summary, state.transactions.size) }
        val parts = state.summary.byCategory.filter { it.second.signum() > 0 }
        if (parts.isNotEmpty()) item { CategoryCard(parts) }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Son işlemler", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.transactions.size > RECENT_COUNT) SeeAllButton(onSeeAll)
            }
        }
        if (state.transactions.isEmpty()) {
            item { EmptyText("Bu ay işlem yok. Hesaplar sekmesinden ekstre yükleyebilirsin.") }
        }
        items(state.transactions.take(RECENT_COUNT), key = { it.id }) { TransactionRow(it) }
    }
}

@Composable
private fun SeeAllButton(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .clip(CircleShape)
            .background(primary.copy(alpha = 0.10f))
            .border(1.5.dp, primary.copy(alpha = 0.8f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Tümü", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = primary)
        Icon(painterResource(Res.drawable.ic_arrow_forward), contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
    }
}

/** Köşeleri yuvarlatılmış dörtgen; köşeler kenarlar boyunca [radius] kadar kesilip eğriyle bağlanır. */
internal fun roundedPolygon(points: List<Offset>, radius: Float): Path = Path().apply {
    fun Offset.unit() = this / getDistance()
    points.forEachIndexed { i, p ->
        val prev = points[(i - 1 + points.size) % points.size]
        val next = points[(i + 1) % points.size]
        val a = p + (prev - p).unit() * radius
        val b = p + (next - p).unit() * radius
        if (i == 0) moveTo(a.x, a.y) else lineTo(a.x, a.y)
        quadraticTo(p.x, p.y, b.x, b.y)
    }
    close()
}

/** Seçili ayın ne kadarının geçtiği: geçmiş aylar dolu, gelecek aylar boş. */
private fun monthProgress(month: YearMonth, today: LocalDate = today()): Float {
    val current = today.yearMonth
    return when {
        month < current -> 1f
        month > current -> 0f
        else -> today.day.toFloat() / month.numberOfDays
    }
}

/** Hafif eğik limon yeşili kart; arkasında daha koyu, daha çok eğilmiş ikinci bir katman durur. */
@Composable
private fun HeroCard(summary: MonthlySummary, count: Int) {
    val extra = AppTheme.extra
    val progress = monthProgress(summary.month)
    Box(
        Modifier
            .fillMaxWidth()
            .height(178.dp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val back = roundedPolygon(
                    listOf(
                        Offset(16.dp.toPx(), 26.dp.toPx()),
                        Offset(w - 2.dp.toPx(), 12.dp.toPx()),
                        Offset(w - 6.dp.toPx(), h - 2.dp.toPx()),
                        Offset(40.dp.toPx(), h - 30.dp.toPx()),
                    ),
                    30.dp.toPx(),
                )
                drawPath(back, extra.heroBack)
                val front = roundedPolygon(
                    listOf(
                        Offset(0f, 10.dp.toPx()),
                        Offset(w, 0f),
                        Offset(w, h - 32.dp.toPx()),
                        Offset(0f, h - 18.dp.toPx()),
                    ),
                    30.dp.toPx(),
                )
                drawPath(front, Brush.linearGradient(extra.heroGradient, Offset.Zero, Offset(w, h)))
            },
    ) {
        Column(
            Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 28.dp, end = 116.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(summary.month.formatMonth(), style = MaterialTheme.typography.titleSmall, color = extra.onHero)
            Text(
                summary.totalSpent.formatMoney(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = extra.onHero,
                maxLines = 1,
                softWrap = false,
            )
            Text("toplam harcama · $count işlem", style = MaterialTheme.typography.bodyLarge, color = extra.onHeroMuted)
        }
        Canvas(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp, end = 22.dp)
                .size(88.dp)
                .semantics { contentDescription = "Ayın yüzde ${(progress * 100).toInt()}'i geçti" },
        ) {
            val stroke = 15.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            drawCircle(extra.heroRingTrack, radius = size.minDimension / 2 - inset, style = Stroke(stroke))
            drawArc(
                extra.heroRingArc,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/** İnce bölümlü çubuk: bir bütünün parçaları. Adlar ve tutarlar altta yazılı, renk tek başına bilgi taşımaz. */
@Composable
private fun CategoryCard(parts: List<Pair<Category, Money>>) {
    val extra = AppTheme.extra
    val total = parts.sumOf { it.second }
    fun share(v: Money) = (v.minor * 100.0 / total.minor).roundToInt()
    val faint = MaterialTheme.colorScheme.onSurface

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Kategoriler",
                Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(
                Modifier
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(10.dp)
                    .semantics {
                        contentDescription = parts.joinToString { (c, v) -> "${c.label} yüzde ${share(v)}" }
                    },
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                parts.forEach { (category, amount) ->
                    Box(
                        Modifier
                            .weight(amount.toDouble().toFloat())
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(category.color(extra)),
                    )
                }
            }
            parts.forEachIndexed { index, (category, amount) ->
                // En büyük kalem vurgulanır.
                val highlight = if (index == 0) Modifier.background(faint.copy(alpha = 0.07f), RoundedCornerShape(18.dp)) else Modifier
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(highlight)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).background(category.color(extra), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(category.icon),
                            contentDescription = null,
                            tint = if (category == Category.OTHER) MaterialTheme.colorScheme.onSurface else extra.onCategory,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(category.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "%${share(amount)}",
                        Modifier
                            .widthIn(min = 52.dp)
                            .background(faint.copy(alpha = 0.07f), CircleShape)
                            .border(1.dp, faint.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        amount.formatMoney(),
                        Modifier.widthIn(min = 96.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
