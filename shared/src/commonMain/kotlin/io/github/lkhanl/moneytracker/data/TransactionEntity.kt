package io.github.lkhanl.moneytracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.lkhanl.moneytracker.core.Category
import io.github.lkhanl.moneytracker.core.Installment
import io.github.lkhanl.moneytracker.core.Kind
import io.github.lkhanl.moneytracker.core.Money
import io.github.lkhanl.moneytracker.core.Source
import io.github.lkhanl.moneytracker.core.Transaction
import kotlinx.datetime.LocalDateTime

/**
 * [amountMinor] kuruş cinsinden işaretli tutardır.
 * [occurredAt] ISO-8601 metin olarak saklanır, bu yüzden metin sıralaması zaman sıralamasıyla aynıdır.
 */
@Entity(tableName = "transactions", indices = [Index("occurredAt")])
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val occurredAt: LocalDateTime,
    val amountMinor: Long,
    val currency: String,
    val description: String,
    val kind: Kind,
    val source: Source,
    val counterparty: String?,
    val installmentCurrent: Int?,
    val installmentTotal: Int?,
    val category: Category,
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    accountId = accountId,
    occurredAt = occurredAt,
    amountMinor = amount.minor,
    currency = currency,
    description = description,
    kind = kind,
    source = source,
    counterparty = counterparty,
    installmentCurrent = installment?.current,
    installmentTotal = installment?.total,
    category = category,
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    accountId = accountId,
    occurredAt = occurredAt,
    amount = Money(amountMinor),
    currency = currency,
    description = description,
    kind = kind,
    source = source,
    counterparty = counterparty,
    installment = if (installmentCurrent != null && installmentTotal != null) {
        Installment(installmentCurrent, installmentTotal)
    } else {
        null
    },
    category = category,
)
