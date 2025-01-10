package com.husnain.authy.di

import android.content.Context
import androidx.room.Room
import com.husnain.authy.data.room.daos.DaoRecentlyDeleted
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.database.TotpDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TotpDatabase {
        return Room.databaseBuilder(
            context,
            TotpDatabase::class.java,
            "totp_database"
        ).build()
    }

    @Provides
    fun provideDaoTotp(database: TotpDatabase): DaoTotp {
        return database.daoTotp()
    }

    @Provides
    fun provideDaoRecentlyDeleted(database: TotpDatabase): DaoRecentlyDeleted {
        return database.daoRecentlyDeleted()
    }
}