package io.github.lkhanl.moneytracker.core

enum class Category { MARKET, FUEL, SUBSCRIPTION, SHOPPING, TRANSFER, OTHER }

/** Banka açıklamasını temizleyip anahtar kelimelerle kategoriye eşler. */
class Categorizer(
    private val rules: List<Pair<Regex, Category>> = DEFAULT_RULES,
) {

    fun merchant(description: String): String = description
        .replace(Regex("""^\d{8}\s+"""), "")
        .replace(Regex("""^(MONEYPAY|IYZICO|PAYTR|PARAM)/""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s+\d+\. Taksit$"""), "")
        .replace(Regex("""\s+[A-ZÇĞİÖŞÜ]+ TRTR$"""), "")
        .trim()

    fun categorize(t: Transaction): Transaction {
        if (t.kind == Kind.TRANSFER_IN || t.kind == Kind.TRANSFER_OUT || t.kind == Kind.INTERNAL_TRANSFER) {
            return t.copy(category = Category.TRANSFER)
        }
        val name = merchant(t.description)
        val category = rules.firstOrNull { (regex, _) -> regex.containsMatchIn(name) }?.second ?: Category.OTHER
        return t.copy(category = category)
    }

    companion object {
        private fun r(p: String) = Regex(p, RegexOption.IGNORE_CASE)

        val DEFAULT_RULES = listOf(
            r("""ENERJI|OPET|SHELL|PETROL|\bBP\b|\bTOTAL\b""") to Category.FUEL,
            r("""GIDA|MARKET|MIGROS|A101|\bBIM\b|\bSOK\b|CARREFOUR""") to Category.MARKET,
            r("""ELEVENLABS|CLAUDE|NETFLIX|SPOTIFY|YOUTUBE|APPLE\.COM|OPENAI""") to Category.SUBSCRIPTION,
            r("""TRENDYOL|AMAZON|HEPSIBURADA|N11""") to Category.SHOPPING,
        )
    }
}
