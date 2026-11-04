package io.github.lkhanl.moneytracker.core

import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth

data class MonthlySummary(
    val month: YearMonth,
    val totalSpent: Money,
    val byCategory: List<Pair<Category, Money>>,
) {
    /** Bu ay en çok para giden kategori. */
    val topCategory: Category? get() = byCategory.firstOrNull()?.first

    companion object {
        /** İç transferler ve kart borcu ödemeleri harcama sayılmaz; iadeler harcamadan düşer. */
        fun of(month: YearMonth, transactions: List<Transaction>): MonthlySummary {
            val spending = transactions.filter {
                it.occurredAt.date.yearMonth == month &&
                    it.kind in setOf(Kind.PURCHASE, Kind.REFUND, Kind.TRANSFER_OUT)
            }
            val byCategory = spending
                .groupBy { it.category }
                .mapValues { (_, ts) -> ts.sumOf { it.amount }.negate() }
                .toList()
                .sortedByDescending { it.second }
            return MonthlySummary(month, byCategory.sumOf { it.second }, byCategory)
        }
    }
}
