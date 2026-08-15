package com.example.nhatkyduonghuyet.ml;

import android.content.Context;
import android.util.Log;
import com.example.nhatkyduonghuyet.ai.Normalizer;
import com.example.nhatkyduonghuyet.ai.PredictionOutcome;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\b\u0007\u0018\u0000 \u00142\u00020\u0001:\u0001\u0014B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\fH\u0002J$\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000e2\u0006\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u0012H\u0086@\u00a2\u0006\u0002\u0010\u0013R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0015"}, d2 = {"Lcom/example/nhatkyduonghuyet/ml/GlucosePredictor;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "inferenceMutex", "Lkotlinx/coroutines/sync/Mutex;", "interpreter", "Lorg/tensorflow/lite/Interpreter;", "loadModelFile", "Ljava/nio/MappedByteBuffer;", "filename", "", "predict", "Lcom/example/nhatkyduonghuyet/ai/PredictionOutcome;", "", "fasting", "type", "", "(FILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public final class GlucosePredictor {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.sync.Mutex inferenceMutex = null;
    @org.jetbrains.annotations.Nullable()
    private org.tensorflow.lite.Interpreter interpreter;
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String TAG = "GlucosePredictor";
    @org.jetbrains.annotations.NotNull()
    private static final com.example.nhatkyduonghuyet.ml.GlucosePredictor.Companion Companion = null;
    
    @javax.inject.Inject()
    public GlucosePredictor(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object predict(float fasting, int type, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.nhatkyduonghuyet.ai.PredictionOutcome<java.lang.Float>> $completion) {
        return null;
    }
    
    private final java.nio.MappedByteBuffer loadModelFile(android.content.Context context, java.lang.String filename) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/example/nhatkyduonghuyet/ml/GlucosePredictor$Companion;", "", "()V", "TAG", "", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
}