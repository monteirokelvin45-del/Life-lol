package com.lifeos.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifeos.core.database.converter.LifeOsTypeConverters
import com.lifeos.core.database.dao.LifeOsDao
import com.lifeos.core.database.entity.BookEntity
import com.lifeos.core.database.entity.BookQuoteEntity
import com.lifeos.core.database.entity.CommitmentContractEntity
import com.lifeos.core.database.entity.ExerciseEntity
import com.lifeos.core.database.entity.FinancialRecordEntity
import com.lifeos.core.database.entity.GhostSquadMemberEntity
import com.lifeos.core.database.entity.HabitEntity
import com.lifeos.core.database.entity.HabitLogEntity
import com.lifeos.core.database.entity.StudyTopicEntity
import com.lifeos.core.database.entity.WorkoutRoutineEntity
import com.lifeos.core.database.security.DatabasePassphraseProvider
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import net.sqlcipher.database.SupportFactory

/**
 * Banco de dados relacional central Local-First do Life OS.
 *
 * - Criptografado integralmente em repouso via SQLCipher 4 (AES-256-CBC + HMAC-SHA512).
 * - Chave de 256 bits protegida por envelope AES-256-GCM no Android Keystore.
 * - Operação em modo Write-Ahead Logging (WAL) para leituras reativas concorrentes a 120 FPS.
 */
@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        WorkoutRoutineEntity::class,
        ExerciseEntity::class,
        StudyTopicEntity::class,
        FinancialRecordEntity::class,
        BookEntity::class,
        BookQuoteEntity::class,
        CommitmentContractEntity::class,
        GhostSquadMemberEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(LifeOsTypeConverters::class)
abstract class LifeOsDatabase : RoomDatabase() {

    abstract fun lifeOsDao(): LifeOsDao

    companion object {
        const val DATABASE_NAME = "life_os_encrypted.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `commitment_contracts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `weekly_target_ratio` REAL NOT NULL DEFAULT 0.8,
                        `stake_amount` REAL NOT NULL,
                        `currency_code` TEXT NOT NULL DEFAULT 'MZN',
                        `institution_name` TEXT NOT NULL,
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `created_at_epoch_day` INTEGER NOT NULL DEFAULT 0,
                        `last_audited_week_epoch` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_commitment_contracts_is_active` ON `commitment_contracts` (`is_active`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ghost_squad_members` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `slot_index` INTEGER NOT NULL,
                        `codename` TEXT NOT NULL,
                        `focus_domain` TEXT NOT NULL,
                        `aura_hex_color` TEXT NOT NULL,
                        `base_minutes_today` INTEGER NOT NULL DEFAULT 45
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_ghost_squad_members_slot_index` ON `ghost_squad_members` (`slot_index`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: LifeOsDatabase? = null

        fun getInstance(
            context: Context,
            passphraseProvider: DatabasePassphraseProvider = DatabasePassphraseProvider(context.applicationContext)
        ): LifeOsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildEncryptedDatabase(
                    context = context.applicationContext,
                    passphraseProvider = passphraseProvider
                ).also { INSTANCE = it }
            }
        }

        fun buildEncryptedDatabase(
            context: Context,
            passphraseProvider: DatabasePassphraseProvider
        ): LifeOsDatabase {
            // Inicializa binários nativos JNI do SQLCipher antes da abertura do arquivo
            SQLiteDatabase.loadLibs(context)

            val passphraseBytes = passphraseProvider.getOrCreatePassphrase()

            val cipherHook = object : SQLiteDatabaseHook {
                override fun preKey(database: SQLiteDatabase?) = Unit

                override fun postKey(database: SQLiteDatabase?) {
                    // Sanitiza páginas de memória liberadas pelo SQLite e reforça integridade referencial
                    database?.rawExecSQL("PRAGMA cipher_memory_security = ON;")
                    database?.rawExecSQL("PRAGMA foreign_keys = ON;")
                }
            }

            // clearPassphrase = true zera automaticamente o ByteArray da chave em RAM após abrir o DB
            val supportFactory = SupportFactory(
                passphraseBytes,
                cipherHook,
                true
            )

            return Room.databaseBuilder(
                context,
                LifeOsDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(supportFactory)
                .addMigrations(MIGRATION_1_2)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }

        /**
         * Fábrica auxiliar em memória para testes instrumentados de DAOs e migrações.
         */
        fun buildInMemoryForTesting(context: Context): LifeOsDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                LifeOsDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
