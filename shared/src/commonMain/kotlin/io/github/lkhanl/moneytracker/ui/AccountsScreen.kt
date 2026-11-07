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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import io.github.lkhanl.moneytracker.resources.ic_delete
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
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
    onImportPdf: () -> Unit,
    onAddAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    padding: PaddingValues,
) {
    var addingAccount by rememberSaveable { mutableStateOf(false) }
    if (addingAccount) {
        AddAccountSheet(
            existing = state.accounts.mapTo(mutableSetOf()) { it.accountId },
            onAdd = onAddAccount,
            onDismiss = { addingAccount = false },
        )
    }
    // Kartına dokunulan hesap; işlemleri alttan açılan panelde gösterilir.
    var detailAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    detailAccountId?.let { id ->
        AccountTransactionsSheet(
            accountId = id,
            month = state.month,
            transactions = state.transactions.filter { it.accountId == id },
            onImportPdf = onImportPdf,
            onDismiss = { detailAccountId = null },
        )
    }
    // Silme düğmesi açık olan kart; aynı anda yalnızca biri açık olur.
    var openAccount by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            // Boş bir yere (ya da kartın kendisine) dokununca açık kart kapanır. Final aşamada bakılır;
            // böylece silme düğmesi gibi dokunuşu işleyen öğeler önce davranır ve kapatma tetiklenmez.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                    val up = waitForUpOrCancellation(PointerEventPass.Final)
                    if (up != null && !up.isConsumed) openAccount = null
                }
            },
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + NavBarOverhang),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { MonthSelector(state, onSelectMonth) }
        item { Header(onAddAccount = { addingAccount = true }) }
        if (state.accounts.isEmpty()) {
            item { EmptyAccounts() }
        }
        items(state.accounts, key = { it.accountId }) { account ->
            SwipeToDeleteAccount(
                account = account,
                isOpen = openAccount == account.accountId,
                onOpenChange = { open ->
                    openAccount = if (open) account.accountId else openAccount.takeIf { it != account.accountId }
                },
                onDelete = onDeleteAccount,
                onClick = { detailAccountId = account.accountId },
            )
        }
    }
}

@Composable
private fun Header(onAddAccount: () -> Unit) {
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
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(extra.navIndicator)
                .clickable(role = Role.Button, onClickLabel = "Hesap ekle", onClick = onAddAccount),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(Res.drawable.ic_add), contentDescription = "Hesap ekle", tint = extra.onNavIndicator)
        }
    }
}

@Composable
private fun EmptyAccounts() {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerHigh.copy(alpha = 0.72f))
            .border(1.dp, AppTheme.extra.cardBorder, shape)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text("Henüz hesap yok", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Sağ üstteki + ile takip etmek istediğin hesabı ekle.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Alttan açılan hesap ekleme paneli: desteklenen hesap türleri, eklenmiş olanlar pasif. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountSheet(existing: Set<String>, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            then()
            onDismiss()
        }
    }

    if (sheetState.targetValue != SheetValue.Hidden) BlurBackdropWhileShown()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Hesap Ekle", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Eklemek istediğin hesap türünü seç.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            SupportedAccountTypes.forEach { id ->
                val added = id in existing
                val shape = RoundedCornerShape(20.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.surfaceContainerHigh.copy(alpha = 0.72f))
                        .border(1.dp, AppTheme.extra.cardBorder, shape)
                        .clickable(enabled = !added, role = Role.Button) { close { onAdd(id) } }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BankBadge(id)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(accountLabel(id), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("PDF hesap cüzdanından içe aktarılır", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    if (added) {
                        Text("Eklendi", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    } else {
                        Icon(painterResource(Res.drawable.ic_add), contentDescription = null, tint = colors.primary)
                    }
                }
            }
        }
    }
}

/**
 * Listenin uçta artan kaydırması panele geçmesin: hızlı aşağı/yukarı kaydırmada panel
 * kendiliğinden oynamasın. Panel yine tutamacından sürüklenerek kapatılabilir.
 */
private val ConsumeLeftoverScroll = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

/** PDF ekstreden işlem içe aktaran + düğmesi; işlemler sayfasında ve hesap panelinde kullanılır. */
@Composable
internal fun PdfImportButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val extra = AppTheme.extra
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(extra.navIndicator)
            .clickable(role = Role.Button, onClickLabel = "PDF ekstreden içe aktar", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(Res.drawable.ic_add), contentDescription = "PDF ekstreden içe aktar", tint = extra.onNavIndicator)
    }
}

/** Kartın hesabının seçili aydaki işlemleri: ekranın %85'i yüksekliğinde, alttan açılan panel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTransactionsSheet(
    accountId: String,
    month: YearMonth,
    transactions: List<io.github.lkhanl.moneytracker.core.Transaction>,
    onImportPdf: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current

    // İçe aktarma sonucu (toast) ve dosya seçici ana ekranda çıkar; bu yüzden panel önce kapanır.
    fun closeThenImport() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            onImportPdf()
        }
    }
    // Panelin toplam yüksekliği ekranın %85'i olsun diye içerikten sürükleme tutamacının yüksekliği düşülür.
    val sheetHeight = with(density) { (LocalWindowInfo.current.containerSize.height * 0.85f).toDp() } - 48.dp
    val byDay = transactions.groupBy { it.occurredAt.date }

    if (sheetState.targetValue != SheetValue.Hidden) BlurBackdropWhileShown()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
    ) {
        LazyColumn(
            Modifier.fillMaxWidth().height(sheetHeight).nestedScroll(remember { ConsumeLeftoverScroll }),
            // iOS'taki lastik (bounce) efekti panelin sürüklenmesiyle çakışıp aşağı yukarı titretiyordu.
            overscrollEffect = null,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BankBadge(accountId)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(accountLabel(accountId), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${month.formatMonth()} · ${transactions.size} işlem",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    PdfImportButton(::closeThenImport)
                }
            }
            if (byDay.isEmpty()) item { EmptyText("Bu ay işlem yok.") }
            byDay.forEach { (day, items) ->
                item(key = day.toString()) {
                    SectionTitle(day.formatDay(), Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }
                items(items, key = { it.id }) { TransactionRow(it) { time -> time.formatTime() } }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
internal fun ImportDialog(state: ImportState, onDismiss: () -> Unit) {
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
        // Sonuç, MainScreen'de toast (snackbar) olarak gösterilir.
        is ImportState.Done -> Unit
    }
}

/** Kaydırınca açılan silme düğmesinin kart genişliğine oranı ve kartla arasındaki boşluk. */
private const val DeleteButtonFraction = 0.2f
private val DeleteButtonGap = 10.dp

/**
 * Sola kaydırınca kart, sağdaki silme düğmesini gösterecek kadar kayıp durur. Düğmeye basınca hesap
 * silinir; geri kaydırınca kart yerine döner.
 */
@Composable
private fun SwipeToDeleteAccount(
    account: AccountSummary,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0) }
    val buttonWidthPx = widthPx * DeleteButtonFraction
    val openOffset = -(buttonWidthPx + with(density) { DeleteButtonGap.toPx() })
    val offset = remember { Animatable(0f) }
    val dragState = rememberDraggableState { delta ->
        scope.launch { offset.snapTo((offset.value + delta).coerceIn(openOffset, 0f)) }
    }
    // Dışarıdan kapatıldığında (boşluğa dokunma, başka kartın açılması) kart yerine döner.
    LaunchedEffect(isOpen) {
        if (!isOpen && offset.value != 0f) offset.animateTo(0f)
    }
    val progress = if (widthPx == 0) 0f else (offset.value / openOffset).coerceIn(0f, 1f)

    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min).onSizeChanged { widthPx = it.width }) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(with(density) { buttonWidthPx.toDp() })
                .graphicsLayer { alpha = progress }
                .clip(RoundedCornerShape(20.dp))
                .background(colors.errorContainer)
                .clickable(enabled = progress == 1f, role = Role.Button, onClickLabel = "Hesabı sil") {
                    onDelete(account.accountId)
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(painterResource(Res.drawable.ic_delete), contentDescription = null, tint = colors.onErrorContainer)
                Text("Sil", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.onErrorContainer)
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        // Yarıdan fazla açıldıysa ya da hızla sola atıldıysa açık kalır.
                        val open = velocity < -600f || (velocity <= 600f && offset.value < openOffset / 2)
                        onOpenChange(open)
                        offset.animateTo(if (open) openOffset else 0f)
                    },
                ),
        ) {
            Box(
                Modifier.clickable(role = Role.Button, onClickLabel = "İşlemleri göster") {
                    // Silme düğmesi açıkken dokunmak kartı kapatır; kapalıyken işlemleri açar.
                    if (isOpen) onOpenChange(false) else onClick()
                },
            ) { AccountCard(account) }
        }
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
