package io.github.lkhanl.moneytracker.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import io.github.lkhanl.moneytracker.core.Categorizer
import io.github.lkhanl.moneytracker.core.Transaction
import io.github.lkhanl.moneytracker.core.TransferMatcher
import io.github.lkhanl.moneytracker.core.atStartOfDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Ekstre içe aktarmanın sonucu: kaç işlem yeni eklendi, kaçı önceki bir ekstreden vardı ve yenilendi. */
data class ImportResult(val added: Int, val updated: Int)

class TransactionRepository(
    private val db: AppDatabase,
    private val categorizer: Categorizer = Categorizer(),
    private val transferWindow: Duration = 2.minutes,
) {
    private val dao = db.transactions()
    private val accountDao = db.accounts()
    private val matcher = TransferMatcher(transferWindow)

    fun observeAll(): Flow<List<Transaction>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Kullanıcının eklediği hesapların kimlikleri, eklenme sırasıyla. */
    fun observeAccountIds(): Flow<List<String>> = accountDao.observeIds()

    /** Hesabı ekler; zaten ekliyse bir şey yapmaz. */
    suspend fun addAccount(accountId: String) {
        accountDao.insert(AccountEntity(accountId, Clock.System.now().toEpochMilliseconds()))
    }

    /** Hesabı ve ona ait tüm işlemleri siler. */
    suspend fun deleteAccount(accountId: String) = inTransaction {
        dao.deleteByAccount(accountId)
        accountDao.delete(accountId)
    }

    /**
     * İşlemi kaydeder, ardından yakın zamandaki işlemlerle iç transfer eşleştirmesi yapar.
     * Eşleşen çiftin türü ve kategorisi güncellenir.
     */
    suspend fun add(transaction: Transaction) = inTransaction {
        addAccount(transaction.accountId)
        if (dao.insert(categorizer.categorize(transaction).toEntity()) == -1L) return@inTransaction

        val nearby = dao.between(
            transaction.occurredAt.shift(-transferWindow),
            transaction.occurredAt.shift(transferWindow),
        ).map { it.toDomain() }
        val changed = matcher.markInternal(nearby)
            .map(categorizer::categorize)
            .filter { updated -> nearby.first { it.id == updated.id } != updated }
        if (changed.isNotEmpty()) dao.upsert(changed.map { it.toEntity() })
    }

    /**
     * Ekstreden okunan işlemleri kaydeder. Ekstre kapsadığı günler için esas kabul edilir: o günlere ait
     * önceki ekstre kayıtları silinip yenileri yazılır, böylece tekrar yüklemek hem mükerrer kayıt
     * oluşturmaz hem de ayrıştırıcıdaki düzeltmeleri eski kayıtlara uygular.
     */
    suspend fun import(transactions: List<Transaction>): ImportResult = inTransaction {
        var added = 0
        var updated = 0
        for ((accountId, ts) in transactions.groupBy { it.accountId }) {
            // Ekstresi yüklenen hesap henüz eklenmemişse eklenir.
            addAccount(accountId)
            val from = ts.minOf { it.occurredAt }.date.atStartOfDay()
            val to = ts.maxOf { it.occurredAt }.date.plus(1, DateTimeUnit.DAY).atStartOfDay()
            val previous = dao.statementIds(accountId, from, to)
            dao.deleteStatementRows(accountId, from, to)

            val inserted = ts.count { dao.insert(categorizer.categorize(it).toEntity()) != -1L }
            // Eski sürümün ürettiği kimlikler farklı olabildiği için kimlik değil sayı karşılaştırılır.
            val replaced = minOf(inserted, previous.size)
            updated += replaced
            added += inserted - replaced
        }
        ImportResult(added, updated)
    }

    private suspend fun <R> inTransaction(block: suspend () -> R): R =
        db.useWriterConnection { it.immediateTransaction { block() } }

    private fun LocalDateTime.shift(by: Duration): LocalDateTime =
        (toInstant(TimeZone.UTC) + by).toLocalDateTime(TimeZone.UTC)
}
