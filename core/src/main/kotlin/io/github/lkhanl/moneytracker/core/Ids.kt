package io.github.lkhanl.moneytracker.core

import java.security.MessageDigest

internal object Ids {
    fun of(vararg parts: Any?): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(parts.joinToString("|").toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }
}
