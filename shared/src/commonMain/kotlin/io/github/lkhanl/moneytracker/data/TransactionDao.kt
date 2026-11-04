package io.github.lkhanl.moneytracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE occurredAt BETWEEN :from AND :to")
    suspend fun between(from: LocalDateTime, to: LocalDateTime): List<TransactionEntity>

    /** Kimlikler içerikten türetildiği için aynı işlem ikinci kez gelirse yok sayılır. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query(
        "SELECT id FROM transactions WHERE accountId = :accountId AND source = 'STATEMENT' " +
            "AND occurredAt >= :from AND occurredAt < :to"
    )
    suspend fun statementIds(accountId: String, from: LocalDateTime, to: LocalDateTime): List<String>

    @Query(
        "DELETE FROM transactions WHERE accountId = :accountId AND source = 'STATEMENT' " +
            "AND occurredAt >= :from AND occurredAt < :to"
    )
    suspend fun deleteStatementRows(accountId: String, from: LocalDateTime, to: LocalDateTime)

    @Upsert
    suspend fun upsert(transactions: List<TransactionEntity>)
}
