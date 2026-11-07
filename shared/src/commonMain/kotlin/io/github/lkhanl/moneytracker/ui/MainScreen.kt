package io.github.lkhanl.moneytracker.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.github.lkhanl.moneytracker.ui.theme.AppTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import io.github.lkhanl.moneytracker.resources.Res
import io.github.lkhanl.moneytracker.resources.ic_accounts
import io.github.lkhanl.moneytracker.resources.ic_transactions
import io.github.lkhanl.moneytracker.resources.ic_home
import io.github.lkhanl.moneytracker.resources.ic_settings
import io.github.lkhanl.moneytracker.settings.ThemeMode
import kotlinx.datetime.YearMonth

enum class Tab(val label: String, val icon: DrawableResource) {
    HOME("Ana Sayfa", Res.drawable.ic_home),
    HISTORY("İşlemler", Res.drawable.ic_transactions),
    ACCOUNTS("Hesaplar", Res.drawable.ic_accounts),
    SETTINGS("Ayarlar", Res.drawable.ic_settings),
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    state: TransactionsUiState?,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode, Offset) -> Unit,
    versionName: String,
    onSelectMonth: (YearMonth) -> Unit,
    importState: ImportState,
    onImportStatement: (ByteArray) -> Unit,
    onAddAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onDismissImportResult: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val pickPdf = rememberPdfPicker(onImportStatement)
    ImportDialog(importState, onDismissImportResult)
    // İçe aktarma sonucu popup yerine kısa süre görünen toast olarak gelir.
    val snackbarHostState = remember { SnackbarHostState() }
    var toast by remember { mutableStateOf<ImportState.Done?>(null) }
    LaunchedEffect(importState) {
        if (importState is ImportState.Done) {
            toast = importState
            snackbarHostState.showSnackbar(importState.title, duration = SnackbarDuration.Short)
            onDismissImportResult()
        }
    }

    // Diğer sekmelerde geri tuşu önce ana sayfaya döner.
    BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }

    val background = MaterialTheme.colorScheme.background
    val blob = AppTheme.extra.backdropBlob
    val blur = rememberBackdropBlur()
    CompositionLocalProvider(LocalBackdropBlur provides blur) {
    Scaffold(
        modifier = Modifier.backdropBlur(blur).drawBehind {
            drawRect(background)
            // Sağ üstte, ekranın dışına taşan büyük renk lekesi.
            val center = Offset(size.width * 0.95f, size.height * 0.02f)
            val radius = size.width * 0.62f
            drawCircle(
                Brush.radialGradient(listOf(blob, blob.copy(alpha = 0.45f)), center, radius),
                radius,
                center,
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { _ ->
                // Başarıda uygulamanın limon yeşili, hatada kırmızı; kenarlardan boşluklu yüzen toast.
                val colors = MaterialTheme.colorScheme
                val ok = toast?.success ?: true
                val container = if (ok) colors.secondary else colors.errorContainer
                val content = if (ok) colors.onSecondary else colors.onErrorContainer
                Row(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(22.dp))
                        .background(container, RoundedCornerShape(22.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(36.dp).background(content.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (ok) "✓" else "!", color = content, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(toast?.title.orEmpty(), color = content, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(toast?.message.orEmpty(), color = content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        bottomBar = { DropletNavigationBar(selected = tab, onSelect = { tab = it }) },
    ) { scaffoldPadding ->
        // İçerik, damlanın çıkıntı payı kadar menünün arkasına uzanır; çıkıntının yanlarında görünür kalır.
        val padding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding(),
            bottom = (scaffoldPadding.calculateBottomPadding() - NavBarOverhang).coerceAtLeast(0.dp),
        )
        if (state == null && tab != Tab.SETTINGS) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        when (tab) {
            Tab.HOME -> HomeScreen(
                state = state!!,
                onSeeAll = { tab = Tab.HISTORY },
                onSelectMonth = onSelectMonth,
                padding = padding,
            )
            Tab.HISTORY -> HistoryScreen(state!!, onSelectMonth, pickPdf, padding)
            Tab.ACCOUNTS -> AccountsScreen(
                state = state!!,
                onSelectMonth = onSelectMonth,
                onImportPdf = pickPdf,
                onAddAccount = onAddAccount,
                onDeleteAccount = onDeleteAccount,
                padding = padding,
            )
            Tab.SETTINGS -> SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                versionName = versionName,
                padding = padding,
            )
        }
    }
    }
}
