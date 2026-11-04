package io.github.lkhanl.moneytracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lkhanl.moneytracker.core.BankTemplates
import io.github.lkhanl.moneytracker.core.MonthlySummary
import io.github.lkhanl.moneytracker.core.Transaction
import io.github.lkhanl.moneytracker.data.StatementImporter
import io.github.lkhanl.moneytracker.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.github.lkhanl.moneytracker.core.Money
import io.github.lkhanl.moneytracker.core.sumOf
import io.github.lkhanl.moneytracker.data.currentMonth
import kotlinx.datetime.yearMonth
import kotlinx.datetime.YearMonth

/** Bir hesabın seçili aydaki para girişi ve çıkışı. */
data class AccountSummary(
    val accountId: String,
    val inflow: Money,
    val outflow: Money,
    val transactionCount: Int,
    val lastTransaction: Transaction?,
) {
    val net: Money get() = inflow - outflow
}

data class TransactionsUiState(
    val summary: MonthlySummary,
    /** Yalnızca seçili aya ait işlemler, yeniden eskiye. */
    val transactions: List<Transaction>,
    val accounts: List<AccountSummary>,
    /** Gezilebilecek ilk ay: en eski işlemin ayı. */
    val earliestMonth: YearMonth,
    /** Gezilebilecek son ay: içinde bulunulan ay. */
    val latestMonth: YearMonth,
    /** Ay başına toplam harcama; seçicide ayları karşılaştırmak için. */
    val monthlySpent: Map<YearMonth, Money>,
) {
    val month: YearMonth get() = summary.month
}

/** PDF ekstre içe aktarmanın ekranda gösterilen durumu. */
sealed interface ImportState {
    data object Idle : ImportState
    data object Running : ImportState
    data class Done(val title: String, val message: String) : ImportState
}

class TransactionsViewModel(
    repository: TransactionRepository,
    private val importer: StatementImporter,
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val selectedMonth = MutableStateFlow(currentMonth())

    val state: StateFlow<TransactionsUiState?> = combine(repository.observeAll(), selectedMonth) { all, month ->
        val current = currentMonth()
        // En eski işlemin ayından bu aya kadar gezilebilir.
        val earliest = all.minOfOrNull { it.occurredAt.date.yearMonth }?.coerceAtMost(current) ?: current
        TransactionsUiState(
            summary = MonthlySummary.of(month, all),
            transactions = all.filter { it.occurredAt.date.yearMonth == month },
            accounts = accountsOf(month, all),
            earliestMonth = earliest,
            latestMonth = current,
            monthlySpent = all.map { it.occurredAt.date.yearMonth }.distinct()
                .associateWith { MonthlySummary.of(it, all).totalSpent },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectMonth(month: YearMonth) {
        selectedMonth.value = minOf(month, currentMonth())
    }

    fun importStatement(pdf: ByteArray) {
        if (_importState.value == ImportState.Running) return
        _importState.value = ImportState.Running
        viewModelScope.launch {
            val outcome = runCatching { importer.import(pdf) }
            _importState.value = outcome.fold(
                onSuccess = { it.toState() },
                onFailure = { ImportState.Done("İçe aktarılamadı", "PDF okunamadı: ${it.message ?: it::class.simpleName}") },
            )
        }
    }

    fun dismissImportResult() {
        _importState.value = ImportState.Idle
    }

    private fun StatementImporter.Outcome.toState(): ImportState.Done = when (this) {
        is StatementImporter.Outcome.Imported -> {
            val (added, updated) = result
            val message = buildString {
                append(if (added > 0) "$added yeni işlem eklendi." else "Yeni işlem yok.")
                if (updated > 0) append(" Daha önce yüklenmiş $updated işlem yenilendi.")
            }
            ImportState.Done(accountLabel(accountId), message)
        }
        StatementImporter.Outcome.Unrecognized -> ImportState.Done(
            "Tanınmayan PDF",
            "Şimdilik yalnızca GetirFinans hesap cüzdanı PDF'i içe aktarılabiliyor.",
        )
        StatementImporter.Outcome.Empty -> ImportState.Done("İşlem bulunamadı", "Bu hesap cüzdanında hareket yok.")
    }

    private fun accountsOf(month: YearMonth, all: List<Transaction>): List<AccountSummary> {
        val byAccount = all.groupBy { it.accountId }
        // Henüz işlemi olmayan tanımlı hesaplar da listelensin.
        val ids = (KNOWN_ACCOUNTS + byAccount.keys).distinct()
        return ids.map { id ->
            val ts = byAccount[id].orEmpty()
            val inMonth = ts.filter { it.occurredAt.date.yearMonth == month }
            AccountSummary(
                accountId = id,
                inflow = inMonth.filter { it.amount.signum() > 0 }.sumOf { it.amount },
                outflow = inMonth.filter { it.amount.signum() < 0 }.sumOf { it.amount }.negate(),
                transactionCount = inMonth.size,
                lastTransaction = inMonth.maxByOrNull { it.occurredAt },
            )
        }
    }

    private companion object {
        val KNOWN_ACCOUNTS = listOf(BankTemplates.QNB, BankTemplates.GETIR_FINANS, BankTemplates.QNB_CREDIT_CARD)
    }
}
