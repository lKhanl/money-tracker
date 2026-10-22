package io.github.lkhanl.moneytracker.core

import java.math.BigDecimal

object Amounts {
    /**
     * Türk bankalarının farklı tutar yazımlarını okur:
     * "100.00", "100,00", "1.234,56", "1,234.56", "1500,0", "-11000,94".
     * Son ayraç ondalık kabul edilir, ardından en fazla 2 hane gelmesi şartıyla.
     */
    fun parse(raw: String): BigDecimal {
        val s = raw.trim().replace(" ", "")
        require(s.isNotEmpty()) { "Boş tutar" }
        val lastSep = s.indexOfLast { it == '.' || it == ',' }
        val normalized = if (lastSep >= 0 && s.length - lastSep - 1 in 1..2) {
            s.substring(0, lastSep).replace(".", "").replace(",", "") + "." + s.substring(lastSep + 1)
        } else {
            s.replace(".", "").replace(",", "")
        }
        return BigDecimal(normalized).setScale(2)
    }
}
