package io.github.lkhanl.moneytracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lkhanl.moneytracker.resources.Res
import io.github.lkhanl.moneytracker.resources.ic_add
import io.github.lkhanl.moneytracker.resources.ic_bar_chart
import io.github.lkhanl.moneytracker.resources.ic_schedule
import io.github.lkhanl.moneytracker.resources.logo_getir_mark
import io.github.lkhanl.moneytracker.resources.logo_qnb_mark
import io.github.lkhanl.moneytracker.core.BankTemplates
import io.github.lkhanl.moneytracker.ui.theme.AppTheme
import io.github.lkhanl.moneytracker.core.Money
import kotlinx.datetime.YearMonth

@Composable
fun AccountsScreen(
    state: TransactionsUiState,
    onSelectMonth: (YearMonth) -> Unit,
    onShowSupportedBanks: () -> Unit,
    importState: ImportState,
    onImportStatement: (ByteArray) -> Unit,
    onDismissImportResult: () -> Unit,
    padding: PaddingValues,
) {
    val pickPdf = rememberPdfPicker(onImportStatement)
    ImportDialog(importState, onDismissImportResult)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + NavBarOverhang),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { MonthSelector(state, onSelectMonth) }
        item { Header(onImportPdf = pickPdf, onShowSupportedBanks) }
        items(state.accounts, key = { it.accountId }) { AccountCard(it) }
    }
}

@Composable
private fun Header(onImportPdf: () -> Unit, onShowSupportedBanks: () -> Unit) {
    val extra = AppTheme.extra
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Hesaplarım", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Banka ve kart hesaplarını tek yerden takip et.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // PDF ekstreden işlem içe aktarma ya da desteklenen bankaların listesi.
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(extra.navIndicator)
                    .clickable(role = Role.Button, onClickLabel = "Ekleme seçeneklerini aç", onClick = { menuOpen = true }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(Res.drawable.ic_add), contentDescription = "Ekle", tint = extra.onNavIndicator)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("PDF ekstreden içe aktar") },
                    onClick = {
                        menuOpen = false
                        onImportPdf()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Desteklenen bankalar") },
                    onClick = {
                        menuOpen = false
                        onShowSupportedBanks()
                    },
                )
            }
        }
    }
}

@Composable
private fun ImportDialog(state: ImportState, onDismiss: () -> Unit) {
    when (state) {
        ImportState.Idle -> Unit
        ImportState.Running -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("İçe aktarılıyor") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Text("PDF okunuyor…")
                }
            },
        )
        is ImportState.Done -> AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text("Tamam") } },
            title = { Text(state.title) },
            text = { Text(state.message) },
        )
    }
}

/** Kartın vurgu rengi: dönemi artıda kapatan hesaplar limon yeşili, diğerleri mor. */
@Composable
private fun accentFor(account: AccountSummary): Color =
    if (account.net.signum() > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

@Composable
private fun AccountCard(account: AccountSummary) {
    val colors = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    val accent = accentFor(account)
    val positive = account.net.signum() > 0
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerHigh.copy(alpha = 0.72f))
            // Sağ alt köşeden yayılan vurgu ışığı.
            .drawBehind {
                val center = Offset(size.width, size.height)
                val radius = size.width * 0.45f
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = if (positive) 0.35f else 0.45f), Color.Transparent), center, radius),
                    radius,
                    center,
                )
            }
            .border(1.dp, if (positive) accent.copy(alpha = 0.7f) else extra.cardBorder, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BankBadge(account.accountId)
            Text(
                accountLabel(account.accountId),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                account.net.formatMoney(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor(account.net),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Flow("Giriş", account.inflow, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(extra.cardBorder))
            Flow("Çıkış", account.outflow, Modifier.weight(1f).padding(start = 16.dp))
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(Res.drawable.ic_bar_chart), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text(
                    "${account.transactionCount} işlem",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(Res.drawable.ic_schedule),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                account.lastTransaction?.let { "Son işlem: ${it.occurredAt.formatDateTime()}" } ?: "Bu ay işlem yok",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Flow(label: String, amount: Money, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(amount.formatMoney(), style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Bankanın logosu, uygulamanın renklerinde: yuvarlak köşeli renkli kare içinde orijinal logonun
 * tek renkli hâli. Tanınmayan hesaplarda baş harf.
 */
@Composable
private fun BankBadge(accountId: String) {
    val colors = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    val (logo, background, tint) = when (accountId) {
        BankTemplates.QNB, BankTemplates.QNB_CREDIT_CARD ->
            Triple(Res.drawable.logo_qnb_mark, colors.primaryContainer, colors.onPrimaryContainer)
        BankTemplates.GETIR_FINANS -> Triple(Res.drawable.logo_getir_mark, extra.navIndicator, extra.onNavIndicator)
        else -> Triple(null, colors.primaryContainer, colors.onPrimaryContainer)
    }
    Box(
        Modifier.size(52.dp).background(background, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (logo != null) {
            // "g" harfi amblemden daha dolu görünür; biraz küçük çizilir.
            val iconSize = if (logo == Res.drawable.logo_getir_mark) 22.dp else 28.dp
            Icon(painterResource(logo), contentDescription = accountLabel(accountId), tint = tint, modifier = Modifier.size(iconSize))
        } else {
            Text(
                accountLabel(accountId).firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
        }
    }
}
