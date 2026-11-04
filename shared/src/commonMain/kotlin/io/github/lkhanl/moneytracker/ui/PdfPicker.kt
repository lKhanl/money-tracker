package io.github.lkhanl.moneytracker.ui

import androidx.compose.runtime.Composable

/**
 * Sistemin dosya seçicisini PDF'lerle sınırlı açan bir işlev döndürür. Kullanıcı bir dosya seçerse
 * içeriği [onPicked]'e verilir; vazgeçerse hiçbir şey olmaz.
 */
@Composable
expect fun rememberPdfPicker(onPicked: (ByteArray) -> Unit): () -> Unit
