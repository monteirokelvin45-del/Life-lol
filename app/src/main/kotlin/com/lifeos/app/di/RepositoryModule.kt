package com.lifeos.app.di

import com.lifeos.app.data.LifeOsRepositoryImpl
import com.lifeos.core.domain.repository.LifeOsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLifeOsRepository(
        impl: LifeOsRepositoryImpl
    ): LifeOsRepository
}
