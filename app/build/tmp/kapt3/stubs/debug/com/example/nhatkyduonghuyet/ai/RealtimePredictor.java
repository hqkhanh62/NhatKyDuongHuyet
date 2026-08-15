package com.example.nhatkyduonghuyet.ai;

import kotlinx.coroutines.Dispatchers;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nH\u0002J\u000e\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\nH\u0002J\u000e\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\nH\u0002J\u001c\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000f0\n2\u0006\u0010\u0011\u001a\u00020\u0012H\u0086@\u00a2\u0006\u0002\u0010\u0013J\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\r0\nH\u0086@\u00a2\u0006\u0002\u0010\u0015J\"\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00120\u0018H\u0086@\u00a2\u0006\u0002\u0010\u0019R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lcom/example/nhatkyduonghuyet/ai/RealtimePredictor;", "", "model", "Lcom/example/nhatkyduonghuyet/ai/LSTMEngine;", "(Lcom/example/nhatkyduonghuyet/ai/LSTMEngine;)V", "buffer", "Lcom/example/nhatkyduonghuyet/ai/GlucoseBuffer;", "bufferMutex", "Lkotlinx/coroutines/sync/Mutex;", "buildForecastLocked", "Lcom/example/nhatkyduonghuyet/ai/PredictionOutcome;", "Lcom/example/nhatkyduonghuyet/ai/RealtimeForecast;", "buildFutureLocked", "Lcom/example/nhatkyduonghuyet/ai/MultiStepResult;", "buildNextPredictionLocked", "Lcom/example/nhatkyduonghuyet/ai/PredictionResult;", "onNewGlucose", "value", "", "(FLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "predictFuture24Hours", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refresh", "history", "", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class RealtimePredictor {
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.ai.LSTMEngine model = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.nhatkyduonghuyet.ai.GlucoseBuffer buffer = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.sync.Mutex bufferMutex = null;
    
    public RealtimePredictor(@org.jetbrains.annotations.NotNull()
    com.example.nhatkyduonghuyet.ai.LSTMEngine model) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object refresh(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Float> history, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.nhatkyduonghuyet.ai.PredictionOutcome<com.example.nhatkyduonghuyet.ai.RealtimeForecast>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object onNewGlucose(float value, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.nhatkyduonghuyet.ai.PredictionOutcome<com.example.nhatkyduonghuyet.ai.PredictionResult>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object predictFuture24Hours(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.nhatkyduonghuyet.ai.PredictionOutcome<com.example.nhatkyduonghuyet.ai.MultiStepResult>> $completion) {
        return null;
    }
    
    private final com.example.nhatkyduonghuyet.ai.PredictionOutcome<com.example.nhatkyduonghuyet.ai.RealtimeForecast> buildForecastLocked() {
        return null;
    }
    
    private final com.example.nhatkyduonghuyet.ai.PredictionOutcome<com.example.nhatkyduonghuyet.ai.PredictionResult> buildNextPredictionLocked() {
        return null;
    }
    
    private final com.example.nhatkyduonghuyet.ai.PredictionOutcome<com.example.nhatkyduonghuyet.ai.MultiStepResult> buildFutureLocked() {
        return null;
    }
}