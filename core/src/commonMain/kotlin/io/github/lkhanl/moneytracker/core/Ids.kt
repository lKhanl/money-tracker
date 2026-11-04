package io.github.lkhanl.moneytracker.core

import org.kotlincrypto.hash.sha2.SHA256

internal object Ids {
    fun of(vararg parts: Any?): String {
        val digest = SHA256().digest(parts.joinToString("|").encodeToByteArray())
        return digest.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }.take(32)
    }
}
