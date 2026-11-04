package io.github.lkhanl.moneytracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lkhanl.moneytracker.resources.Res
import io.github.lkhanl.moneytracker.resources.ic_expand_more
import io.github.lkhanl.moneytracker.ui.theme.AppTheme
import kotlinx.coroutines.launch
import io.github.lkhanl.moneytracker.core.Money
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth


/** Dar kutular için kısa tutar: ₺330, ₺3,5 bin, ₺1,2 mn. ("B" kısaltması milyar sanılabildiği için yazıyla.) */
private fun Money.formatCompact(): String {
    val v = toDouble()
    return "₺" + when {
        v < 1_000 -> v.toLong().toDouble().formatOneDecimal()
        v < 1_000_000 -> (v / 1_000).formatOneDecimal() + " bin"
        else -> (v / 1_000_000).formatOneDecimal() + " mn"
    }
}

/** Seçili ayı gösterir; basınca dönem seçici açılır. */
@Composable
fun MonthSelector(state: TransactionsUiState, onSelectMonth: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    var picking by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                .border(1.dp, AppTheme.extra.cardBorder, CircleShape)
                .clickable(role = Role.DropdownList, onClickLabel = "Dönem seç") { picking = true }
                .padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.month.formatMonth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                painterResource(Res.drawable.ic_expand_more),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (picking) {
        MonthPickerSheet(
            state = state,
            onSelect = onSelectMonth,
            onDismiss = { picking = false },
        )
    }
}

/**
 * Alttan açılan dönem seçici: üstte yıl çipleri, altta o yılın ayları.
 * Her ay kutusu o ayın harcamasını ve yıl içindeki göreli büyüklüğünü gösterir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthPickerSheet(
    state: TransactionsUiState,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var year by rememberSaveable { mutableIntStateOf(state.month.year) }

    // Panel kapanma animasyonu bittikten sonra kaldırılsın.
    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            then()
            onDismiss()
        }
    }

    // Panel kapanmaya başladığı anda (kapanma animasyonu bitmeden) bulanıklık kalkar.
    if (sheetState.targetValue != SheetValue.Hidden) BlurBackdropWhileShown()

    val blob = AppTheme.extra.backdropBlob
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                // Sağ üstten süzülen mor ışık.
                .drawBehind {
                    val center = Offset(size.width * 1.0f, 0f)
                    val radius = size.width * 0.7f
                    drawCircle(Brush.radialGradient(listOf(blob.copy(alpha = 0.45f), Color.Transparent), center, radius), radius, center)
                }
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Dönem Seç", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "İşlemlerini görmek istediğin dönemi seç.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.month != state.latestMonth) {
                    TextButton(onClick = { close { onSelect(state.latestMonth) } }) { Text("Bu aya dön") }
                }
            }

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                (state.earliestMonth.year..state.latestMonth.year).reversed().forEach { y ->
                    YearChip(y, selected = y == year, onClick = { year = y })
                }
            }

            val months = Month.entries.map { YearMonth(year, it) }
            val yearMax = months.mapNotNull { state.monthlySpent[it] }.maxOrNull()?.takeIf { it.signum() > 0 }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                months.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { month ->
                            MonthCell(
                                month = month,
                                spent = state.monthlySpent[month],
                                share = yearMax?.let { max ->
                                    state.monthlySpent[month]?.let { (it.toDouble() / max.toDouble()).toFloat() }
                                } ?: 0f,
                                selected = month == state.month,
                                isCurrent = month == state.latestMonth,
                                enabled = month in state.earliestMonth..state.latestMonth,
                                onClick = { close { onSelect(month) } },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Yıl seçimi için hap: seçili olan limon yeşili, diğerleri çerçeveli ve saydam. */
@Composable
private fun YearChip(year: Int, selected: Boolean, onClick: () -> Unit) {
    val extra = AppTheme.extra
    val colors = MaterialTheme.colorScheme
    Text(
        year.toString(),
        Modifier
            .clip(CircleShape)
            .background(if (selected) extra.navIndicator else colors.surfaceContainerHigh.copy(alpha = 0.72f))
            .then(if (selected) Modifier else Modifier.border(1.dp, extra.cardBorder, CircleShape))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 22.dp, vertical = 9.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) extra.onNavIndicator else colors.onSurface,
    )
}

@Composable
private fun MonthCell(
    month: YearMonth,
    spent: Money?,
    share: Float,
    selected: Boolean,
    isCurrent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    val shape = RoundedCornerShape(18.dp)
    val background = if (selected) {
        Modifier.background(Brush.linearGradient(extra.heroGradient), shape)
    } else {
        Modifier
            .background(colors.surfaceContainerHigh.copy(alpha = 0.72f), shape)
            .border(1.dp, if (isCurrent) colors.primary else extra.cardBorder, shape)
    }
    val content = if (selected) extra.onHero else colors.onSurface
    val muted = if (selected) extra.onHero else colors.onSurfaceVariant
    val name = month.month.turkishShortName
    val fullName = month.month.turkishName

    Column(
        modifier
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .then(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = buildString {
                    append(fullName)
                    if (isCurrent) append(", bu ay")
                    spent?.let { append(", ${it.formatMoney()} harcama") }
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = content)
        Text(
            when {
                spent != null -> spent.formatCompact()
                enabled -> "İşlem yok"
                else -> "—"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = muted,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        // Yıl içindeki en yüksek harcamaya oranla ince bir çubuk.
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(if (selected) extra.onHero.copy(alpha = 0.2f) else colors.outlineVariant),
        ) {
            if (share > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(share.coerceIn(0.04f, 1f))
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (selected) extra.onHero else colors.primary),
                )
            }
        }
    }
}
