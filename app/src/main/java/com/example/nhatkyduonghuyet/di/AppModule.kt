package com.example.nhatkyduonghuyet.di

import android.content.Context
import androidx.room.Room
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.AppDatabase
import com.example.nhatkyduonghuyet.data.repository.LogRepositoryImpl
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "nhat_ky_duong_huyet_db"
        )
        .build()
    }

    @Provides
    fun provideDao(db: AppDatabase) = db.logEntryDao()
}
