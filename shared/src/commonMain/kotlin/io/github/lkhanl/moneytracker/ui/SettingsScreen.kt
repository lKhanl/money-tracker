package io.github.lkhanl.moneytracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import io.github.lkhanl.moneytracker.core.BankTemplates
import io.github.lkhanl.moneytracker.settings.ThemeMode

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode, Offset) -> Unit,
    versionName: String,
    padding: PaddingValues,
) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + NavBarOverhang)) {
        item { SectionTitle("Görünüm", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        item {
            ListItem(
                colors = transparentListItem,
                headlineContent = { Text("Tema") },
                trailingContent = {
                    // Geçiş animasyonu dokunulan düğmenin ortasından başlasın diye konumları tutulur.
                    val centers = remember { mutableMapOf<ThemeMode, Offset>() }
                    SingleChoiceSegmentedButtonRow {
                        ThemeMode.entries.forEachIndexed { i, mode ->
                            SegmentedButton(
                                modifier = Modifier.onGloballyPositioned {
                                    centers[mode] = it.boundsInRoot().center
                                },
                                selected = mode == themeMode,
                                onClick = { onThemeModeChange(mode, centers[mode] ?: Offset.Zero) },
                                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                            ) { Text(mode.label) }
                        }
                    }
                },
            )
        }
        item { HorizontalDivider() }
        item { SectionTitle("Desteklenen bankalar", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        item {
            ListItem(
                colors = transparentListItem,
                headlineContent = { Text(accountLabel(BankTemplates.GETIR_FINANS)) },
                supportingContent = { Text("PDF hesap cüzdanı") },
            )
        }
        item { HorizontalDivider() }
        item {
            Column(Modifier.padding(16.dp)) {
                Text("Money Tracker", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Sürüm $versionName · Veriler yalnızca bu cihazda saklanır",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Satırlar zemini kendisi boyamaz; arkadaki renk lekesi kesilmeden görünür. */
private val transparentListItem
    @Composable get() = ListItemDefaults.colors(containerColor = Color.Transparent)
