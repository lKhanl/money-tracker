package io.github.lkhanl.moneytracker.core

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Kendi hesapların arasındaki transferleri bulur: bir hesaptan çıkan ve
 * başka bir hesaba [window] içinde giren aynı tutar iç transfer sayılır.
 */
class TransferMatcher(private val window: Duration = 2.minutes) {

    fun markInternal(transactions: List<Transaction>): List<Transaction> {
        val outs = transactions.filter { it.kind == Kind.TRANSFER_OUT }
        val ins = transactions.filter { it.kind == Kind.TRANSFER_IN }.toMutableList()
        val internal = mutableSetOf<String>()
        for (out in outs.sortedBy { it.occurredAt }) {
            val match = ins.firstOrNull {
                it.accountId != out.accountId &&
                    it.amount.compareTo(out.amount.negate()) == 0 &&
                    (it.occurredAt.toInstant(TimeZone.UTC) - out.occurredAt.toInstant(TimeZone.UTC)).absoluteValue <= window
            } ?: continue
            ins.remove(match)
            internal += out.id
            internal += match.id
        }
        return transactions.map { if (it.id in internal) it.copy(kind = Kind.INTERNAL_TRANSFER) else it }
    }
}
