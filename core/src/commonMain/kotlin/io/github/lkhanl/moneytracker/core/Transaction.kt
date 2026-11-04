package io.github.lkhanl.moneytracker.core

import kotlinx.datetime.LocalDateTime

enum class Source { STATEMENT }

enum class Kind {
    PURCHASE,
    TRANSFER_OUT,
    TRANSFER_IN,
    CARD_PAYMENT,
    REFUND,
    INTERNAL_TRANSFER,
}

data class Installment(val current: Int, val total: Int)

/**
 * Tek bir para hareketi. [amount] işaretlidir: eksi = paranın çıkışı, artı = girişi.
 */
data class Transaction(
    val id: String,
    val accountId: String,
    val occurredAt: LocalDateTime,
    val amount: Money,
    val currency: String,
    val description: String,
    val kind: Kind,
    val source: Source,
    val counterparty: String? = null,
    val installment: Installment? = null,
    val category: Category = Category.OTHER,
)
