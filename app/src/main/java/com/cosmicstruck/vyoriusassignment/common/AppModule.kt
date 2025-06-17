package com.cosmicstruck.vyoriusassignment.common

import android.content.Context
import com.pedro.common.ConnectChecker
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
    fun provideConnectionChecker(): ConnectChecker {
        return ConnectCheckerImpl()
    }
}