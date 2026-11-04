package io.github.lkhanl.moneytracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lkhanl.moneytracker.resources.Res
import io.github.lkhanl.moneytracker.resources.ic_cat_transfer
import io.github.lkhanl.moneytracker.resources.ic_kind_card
import io.github.lkhanl.moneytracker.resources.ic_kind_in
import io.github.lkhanl.moneytracker.resources.ic_kind_out
import io.github.lkhanl.moneytracker.resources.ic_kind_refund
import io.github.lkhanl.moneytracker.core.Kind
import io.github.lkhanl.moneytracker.core.Transaction
import io.github.lkhanl.moneytracker.ui.theme.AppTheme
import io.github.lkhanl.moneytracker.ui.theme.color
import io.github.lkhanl.moneytracker.core.Money
import kotlinx.datetime.LocalDateTime

@Composable
fun amountColor(amount: Money): Color =
    if (amount.signum() > 0) AppTheme.extra.income else MaterialTheme.colorScheme.onSurface

/** Uygulamadaki standart kart: yarı saydam yüzey, ince çerçeve; arkadaki renk lekesi hafifçe görünür. */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)),
        border = BorderStroke(1.dp, AppTheme.extra.cardBorder),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** Dolu renkli zemin üzerinde baş harf. */
@Composable
fun Avatar(
    text: String,
    tint: Color = MaterialTheme.colorScheme.primaryContainer,
    onTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 44.dp,
) {
    Box(
        Modifier.size(size).background(tint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.firstOrNull { it.isLetter() }?.uppercase() ?: "₺",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = onTint,
        )
    }
}

/** İşlemin yönünü gösteren küçük rozet: para girişleri limon yeşili, çıkışlar mor. */
@Composable
fun KindBadge(kind: Kind) {
    val colors = MaterialTheme.colorScheme
    val (icon, tint) = when (kind) {
        Kind.TRANSFER_IN -> Res.drawable.ic_kind_in to colors.secondary
        Kind.REFUND -> Res.drawable.ic_kind_refund to colors.secondary
        Kind.PURCHASE -> Res.drawable.ic_kind_card to colors.secondary
        Kind.TRANSFER_OUT -> Res.drawable.ic_kind_out to colors.primary
        Kind.CARD_PAYMENT -> Res.drawable.ic_kind_card to colors.primary
        Kind.INTERNAL_TRANSFER -> Res.drawable.ic_cat_transfer to colors.primary
    }
    Box(
        Modifier.size(40.dp).background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = kind.label, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun TransactionRow(t: Transaction, formatTime: (LocalDateTime) -> String = { it.formatDateTime() }) {
    val name = t.counterparty ?: t.description
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
            .border(1.dp, AppTheme.extra.cardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KindBadge(t.kind)
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatTime(t.occurredAt)} · ${accountLabel(t.accountId)} · ${t.kind.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            t.amount.formatMoney(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            // İç transfer net etkisizdir; vurgulanmasın.
            color = if (t.kind == Kind.INTERNAL_TRANSFER) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                amountColor(t.amount)
            },
        )
    }
}

@Composable
fun EmptyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
