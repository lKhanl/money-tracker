package io.github.lkhanl.moneytracker.data

import io.github.lkhanl.moneytracker.core.GetirFinansStatementParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** PDF dosyasından düz metin çıkarır; her platform kendi PDF kütüphanesini kullanır. */
fun interface PdfTextExtractor {
    fun extract(pdf: ByteArray): String
}

/** Kullanıcının seçtiği PDF ekstreyi tanır, işlemlerini okur ve kaydeder. */
class StatementImporter(
    private val pdfText: PdfTextExtractor,
    private val repository: TransactionRepository,
) {
    sealed interface Outcome {
        data class Imported(val accountId: String, val result: ImportResult) : Outcome
        /** PDF okundu ama desteklenen bir ekstre değil. */
        data object Unrecognized : Outcome
        /** Ekstre tanındı ama içinde hareket yok. */
        data object Empty : Outcome
    }

    private val getirFinans = GetirFinansStatementParser()

    suspend fun import(pdf: ByteArray): Outcome {
        val text = withContext(Dispatchers.IO) { pdfText.extract(pdf) }
        if (!getirFinans.matches(text)) return Outcome.Unrecognized
        val transactions = getirFinans.parse(text)
        if (transactions.isEmpty()) return Outcome.Empty
        return Outcome.Imported(transactions.first().accountId, repository.import(transactions))
    }
}
