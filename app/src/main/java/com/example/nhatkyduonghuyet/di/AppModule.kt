package com.example.nhatkyduonghuyet.di

import android.content.Context
import androidx.room.Room
import com.example.nhatkyduonghuyet.data.local.AppDatabase
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.dao.MedicationDao
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
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao(db: AppDatabase): LogEntryDao = db.logEntryDao()

    @Provides
    fun provideMedicationDao(db: AppDatabase): MedicationDao = db.medicationDao()
}