package io.github.lkhanl.moneytracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.YearMonth

@Composable
fun HistoryScreen(
    state: TransactionsUiState,
    onSelectMonth: (YearMonth) -> Unit,
    padding: PaddingValues,
) {
    val byDay = state.transactions.groupBy { it.occurredAt.date }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp + NavBarOverhang),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { MonthSelector(state, onSelectMonth) }
        if (byDay.isEmpty()) item { EmptyText("Bu ay işlem yok.") }
        byDay.forEach { (day, transactions) ->
            item(key = day.toString()) {
                SectionTitle(
                    day.formatDay(),
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            }
            items(transactions, key = { it.id }) { TransactionRow(it) { time -> time.formatTime() } }
        }
    }
}
