package com.example.nhatkyduonghuyet.di

import android.content.Context
import androidx.room.Room
import com.example.nhatkyduonghuyet.data.local.AppDatabase
import com.example.nhatkyduonghuyet.data.local.DatabaseMigrations
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
            // Real migrations instead of fallbackToDestructiveMigration():
            // the diary and the medication history must survive an app update.
            .addMigrations(*DatabaseMigrations.ALL)
            // Only a *downgrade* (installing an older APK over a newer one)
            // rebuilds the file. Upgrades never drop data.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideDao(db: AppDatabase): LogEntryDao = db.logEntryDao()

    @Provides
    fun provideMedicationDao(db: AppDatabase): MedicationDao = db.medicationDao()
}