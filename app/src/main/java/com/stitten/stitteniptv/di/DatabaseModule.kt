package com.stitten.stitteniptv.di

import android.content.Context
import androidx.room.Room
import com.stitten.stitteniptv.database.AppDatabase
import com.stitten.stitteniptv.database.dao.ChannelDao
import com.stitten.stitteniptv.database.dao.ErrorLogDao
import com.stitten.stitteniptv.database.dao.ParentalBlockDao
import com.stitten.stitteniptv.database.dao.SourceDao
import com.stitten.stitteniptv.database.dao.WatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "stitten_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWatchHistoryDao(db: AppDatabase): WatchHistoryDao = db.watchHistoryDao()

    @Provides
    fun provideErrorLogDao(db: AppDatabase): ErrorLogDao = db.errorLogDao()

    @Provides
    fun provideParentalBlockDao(db: AppDatabase): ParentalBlockDao = db.parentalBlockDao()

    @Provides
    fun provideSourceDao(db: AppDatabase): SourceDao = db.sourceDao()

    @Provides
    fun provideChannelDao(db: AppDatabase): ChannelDao = db.channelDao()
}
