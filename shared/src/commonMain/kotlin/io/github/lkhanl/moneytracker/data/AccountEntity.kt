package io.github.lkhanl.moneytracker.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Kullanıcının eklediği hesap. [id] hesap türünün kimliğidir (ör. [io.github.lkhanl.moneytracker.core.BankTemplates.GETIR_FINANS]);
 * işlemler bu kimlikle hesaba bağlanır. [createdAt] listeleme sırası için Unix zamanı (ms).
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
)

@Dao
interface AccountDao {

    @Query("SELECT id FROM accounts ORDER BY createdAt, id")
    fun observeIds(): Flow<List<String>>

    /** Hesap zaten varsa yok sayılır. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(account: AccountEntity): Long

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: String)
}
