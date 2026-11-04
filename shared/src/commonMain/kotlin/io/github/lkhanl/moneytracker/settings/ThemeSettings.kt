package io.github.lkhanl.moneytracker.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) { DARK("Koyu"), LIGHT("Açık") }

/** Basit anahtar-değer deposu: Android'de SharedPreferences, iOS'ta NSUserDefaults. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

class ThemeSettings(private val store: KeyValueStore) {
    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        store.putString(KEY, mode.name)
        _mode.value = mode
    }

    private fun read(): ThemeMode =
        store.getString(KEY)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK

    private companion object {
        const val KEY = "theme_mode"
    }
}
