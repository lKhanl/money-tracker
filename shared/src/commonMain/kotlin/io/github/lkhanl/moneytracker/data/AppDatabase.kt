package io.github.lkhanl.moneytracker.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [TransactionEntity::class, AccountEntity::class], version = 3)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactions(): TransactionDao
    abstract fun accounts(): AccountDao

    companion object {
        const val FILE_NAME = "money-tracker.db"

        /** Dosya konumu platforma göre belirlenir; gerisi her yerde aynı. */
        fun create(builder: Builder<AppDatabase>): AppDatabase = builder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

        /**
         * Bildirimden okuma kaldırıldı. Eski sürümde bildirimden yakalanan kayıtlar silinmez, ekstre kaydı
         * sayılır; aynı günü kapsayan bir ekstre yüklendiğinde ekstredeki kayıtlarla değiştirilir.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("UPDATE transactions SET source = 'STATEMENT' WHERE source = 'NOTIFICATION'")
            }
        }

        /**
         * Hesaplar artık sabit değil, kullanıcı ekliyor. İşlemi olan hesaplar kaybolmasın diye
         * mevcut işlemlerin hesapları otomatik eklenir.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                connection.execSQL(
                    "INSERT OR IGNORE INTO accounts (id, createdAt) SELECT accountId, 0 FROM transactions GROUP BY accountId"
                )
            }
        }
    }
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
