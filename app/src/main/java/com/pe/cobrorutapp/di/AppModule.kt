package com.pe.cobrorutapp.di

import android.content.Context
import com.pe.cobrorutapp.core.data.preferences.DataStoreUserPreferencesRepository
import com.pe.cobrorutapp.core.domain.preferences.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository =
        DataStoreUserPreferencesRepository(context)
}
