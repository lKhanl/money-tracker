package io.github.lkhanl.moneytracker.core

import java.math.BigDecimal
import java.time.YearMonth

data class MonthlySummary(
    val month: YearMonth,
    val totalSpent: BigDecimal,
    val byCategory: List<Pair<Category, BigDecimal>>,
) {
    /** Bu ay en çok para giden kategori. */
    val topCategory: Category? get() = byCategory.firstOrNull()?.first

    companion object {
        /** İç transferler ve kart borcu ödemeleri harcama sayılmaz; iadeler harcamadan düşer. */
        fun of(month: YearMonth, transactions: List<Transaction>): MonthlySummary {
            val spending = transactions.filter {
                YearMonth.from(it.occurredAt) == month &&
                    it.kind in setOf(Kind.PURCHASE, Kind.REFUND, Kind.TRANSFER_OUT)
            }
            val byCategory = spending
                .groupBy { it.category }
                .mapValues { (_, ts) -> ts.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }.negate() }
                .toList()
                .sortedByDescending { it.second }
            return MonthlySummary(month, byCategory.fold(BigDecimal.ZERO) { a, p -> a + p.second }, byCategory)
        }
    }
}
