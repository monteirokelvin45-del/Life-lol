package com.lifeos.core.database.di

import android.content.Context
import com.lifeos.core.database.LifeOsDatabase
import com.lifeos.core.database.dao.LifeOsDao
import com.lifeos.core.database.security.DatabasePassphraseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt do pacote `:core:database` disponibilizando singletons thread-safe
 * do banco criptografado SQLCipher e da interface reativa `LifeOsDao`.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabasePassphraseProvider(
        @ApplicationContext context: Context
    ): DatabasePassphraseProvider = DatabasePassphraseProvider(context)

    @Provides
    @Singleton
    fun provideLifeOsDatabase(
        @ApplicationContext context: Context,
        passphraseProvider: DatabasePassphraseProvider
    ): LifeOsDatabase = LifeOsDatabase.getInstance(
        context = context,
        passphraseProvider = passphraseProvider
    )

    @Provides
    @Singleton
    fun provideLifeOsDao(
        database: LifeOsDatabase
    ): LifeOsDao = database.lifeOsDao()
}
