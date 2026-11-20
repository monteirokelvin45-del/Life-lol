package com.lifeos.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifeos.core.database.converter.LifeOsTypeConverters
import com.lifeos.core.database.dao.LifeOsDao
import com.lifeos.core.database.entity.BookEntity
import com.lifeos.core.database.entity.ExerciseEntity
import com.lifeos.core.database.entity.FinancialRecordEntity
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
        BookEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(LifeOsTypeConverters::class)
abstract class LifeOsDatabase : RoomDatabase() {

    abstract fun lifeOsDao(): LifeOsDao

    companion object {
        const val DATABASE_NAME = "life_os_encrypted.db"

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
