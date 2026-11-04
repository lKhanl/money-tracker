package io.github.lkhanl.moneytracker.core

import kotlin.jvm.JvmInline
import kotlin.math.abs

/**
 * Kuruş cinsinden işaretli tutar. Tüm platformlarda aynı çalışsın diye BigDecimal yerine kullanılır;
 * uygulamadaki tutarların hepsi iki ondalıklı olduğu için kayıp yoktur.
 */
@JvmInline
value class Money(val minor: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(minor + other.minor)
    operator fun minus(other: Money) = Money(minor - other.minor)
    operator fun unaryMinus() = Money(-minor)
    fun negate() = -this
    fun abs() = Money(abs(minor))
    fun signum(): Int = minor.compareTo(0)
    fun toDouble(): Double = minor / 100.0

    override fun compareTo(other: Money): Int = minor.compareTo(other.minor)

    /** "1234.56", "-0.50": işlem kimliklerinde kullanıldığı için biçimi değişmemeli. */
    override fun toString(): String {
        val sign = if (minor < 0) "-" else ""
        val a = abs(minor)
        return "$sign${a / 100}.${(a % 100).toString().padStart(2, '0')}"
    }

    companion object {
        val ZERO = Money(0)

        /** "1234.56" gibi noktalı metinden; en fazla iki ondalık. */
        fun parse(plain: String): Money {
            val negative = plain.startsWith("-")
            val digits = plain.removePrefix("-").removePrefix("+")
            val whole = digits.substringBefore('.')
            val fraction = digits.substringAfter('.', "").padEnd(2, '0')
            require(whole.isNotEmpty() || fraction.isNotEmpty()) { "Geçersiz tutar: $plain" }
            require(fraction.length == 2 && (whole + fraction).all { it.isDigit() }) { "Geçersiz tutar: $plain" }
            val minor = (whole.ifEmpty { "0" }.toLong()) * 100 + fraction.toLong()
            return Money(if (negative) -minor else minor)
        }

        fun ofWhole(value: Long) = Money(value * 100)
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO) { acc, m -> acc + m }

inline fun <T> Iterable<T>.sumOf(selector: (T) -> Money): Money = fold(Money.ZERO) { acc, t -> acc + selector(t) }
