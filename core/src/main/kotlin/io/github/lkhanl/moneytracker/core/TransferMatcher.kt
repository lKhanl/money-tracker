package io.github.lkhanl.moneytracker.core

import java.time.Duration

/**
 * Kendi hesapların arasındaki transferleri bulur: bir hesaptan çıkan ve
 * başka bir hesaba [window] içinde giren aynı tutar iç transfer sayılır.
 */
class TransferMatcher(private val window: Duration = Duration.ofMinutes(2)) {

    fun markInternal(transactions: List<Transaction>): List<Transaction> {
        val outs = transactions.filter { it.kind == Kind.TRANSFER_OUT }
        val ins = transactions.filter { it.kind == Kind.TRANSFER_IN }.toMutableList()
        val internal = mutableSetOf<String>()
        for (out in outs.sortedBy { it.occurredAt }) {
            val match = ins.firstOrNull {
                it.accountId != out.accountId &&
                    it.amount.compareTo(out.amount.negate()) == 0 &&
                    Duration.between(out.occurredAt, it.occurredAt).abs() <= window
            } ?: continue
            ins.remove(match)
            internal += out.id
            internal += match.id
        }
        return transactions.map { if (it.id in internal) it.copy(kind = Kind.INTERNAL_TRANSFER) else it }
    }
}
