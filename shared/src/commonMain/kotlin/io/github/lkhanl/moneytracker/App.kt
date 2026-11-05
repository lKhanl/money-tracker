package io.github.lkhanl.moneytracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.RoomDatabase
import io.github.lkhanl.moneytracker.data.AppDatabase
import io.github.lkhanl.moneytracker.data.DemoData
import io.github.lkhanl.moneytracker.data.PdfTextExtractor
import io.github.lkhanl.moneytracker.data.StatementImporter
import io.github.lkhanl.moneytracker.data.TransactionRepository
import io.github.lkhanl.moneytracker.settings.KeyValueStore
import io.github.lkhanl.moneytracker.settings.ThemeMode
import io.github.lkhanl.moneytracker.settings.ThemeSettings
import io.github.lkhanl.moneytracker.ui.MainScreen
import io.github.lkhanl.moneytracker.ui.SplashScreen
import io.github.lkhanl.moneytracker.ui.TransactionsViewModel
import io.github.lkhanl.moneytracker.ui.theme.MoneyTrackerTheme
import io.github.lkhanl.moneytracker.ui.theme.ThemeModeChange
import io.github.lkhanl.moneytracker.ui.theme.ThemeReveal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Uygulama boyunca tek örneği olan nesneler. Platformdan yalnızca platforma özel parçalar gelir. */
class AppGraph(
    databaseBuilder: RoomDatabase.Builder<AppDatabase>,
    settingsStore: KeyValueStore,
    pdfText: PdfTextExtractor,
    val versionName: String,
    demoData: Boolean = false,
) {
    val repository: TransactionRepository by lazy { TransactionRepository(AppDatabase.create(databaseBuilder)) }
    val statementImporter: StatementImporter by lazy { StatementImporter(pdfText, repository) }
    val themeSettings = ThemeSettings(settingsStore)

    init {
        // Örnek veri yalnızca geliştirme derlemelerinde eklenir. Kimlikler sabit olduğundan tekrar eklenmez.
        if (demoData) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                DemoData.generate().forEach { repository.add(it) }
            }
        }
    }
}

/**
 * Uygulamanın kökü. [onDarkThemeChanged] platformun sistem çubuklarını seçili temaya uydurması içindir;
 * tema sistemden bağımsız seçildiği için bunu Compose kendisi yapamaz.
 */
@Composable
fun App(graph: AppGraph, onDarkThemeChanged: (Boolean) -> Unit = {}) {
    val themeMode by graph.themeSettings.mode.collectAsStateWithLifecycle()
    val dark = themeMode == ThemeMode.DARK
    LaunchedEffect(dark) { onDarkThemeChanged(dark) }

    // Açılış ekranı yalnızca uygulama ilk açıldığında; döndürmede tekrar gösterilmez.
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val vm: TransactionsViewModel = viewModel(
        factory = viewModelFactory { initializer { TransactionsViewModel(graph.repository, graph.statementImporter) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val importState by vm.importState.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        ThemeReveal(onApply = { graph.themeSettings.set(it.mode) }) { switchTheme ->
            MoneyTrackerTheme(dark) {
                MainScreen(
                    state = state,
                    themeMode = themeMode,
                    onThemeModeChange = { mode, origin ->
                        if (mode != themeMode) switchTheme(ThemeModeChange(mode, origin))
                    },
                    versionName = graph.versionName,
                    onSelectMonth = vm::selectMonth,
                    importState = importState,
                    onImportStatement = vm::importStatement,
                    onAddAccount = vm::addAccount,
                    onDeleteAccount = vm::deleteAccount,
                    onDismissImportResult = vm::dismissImportResult,
                )
            }
        }
        AnimatedVisibility(visible = showSplash, enter = EnterTransition.None, exit = fadeOut(tween(350))) {
            SplashScreen(ready = state != null, onFinished = { showSplash = false })
        }
    }
}
