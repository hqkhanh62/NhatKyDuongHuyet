package com.example.nhatkyduonghuyet.di

import android.content.Context
import com.example.nhatkyduonghuyet.ai.LSTMEngine
import com.example.nhatkyduonghuyet.ai.RealtimePredictor
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideLSTMEngine(@ApplicationContext context: Context): LSTMEngine {
        return LSTMEngine(context)
    }

    @Provides
    @Singleton
    fun provideRealtimePredictor(engine: LSTMEngine): RealtimePredictor {
        return RealtimePredictor(engine)
    }

    @Provides
    @Singleton
    fun provideGlucoseScanner(): GlucoseScanner {
        return GlucoseScanner()
    }
}
