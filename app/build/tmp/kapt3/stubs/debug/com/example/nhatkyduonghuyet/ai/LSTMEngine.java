package com.example.nhatkyduonghuyet.ai;

import android.content.Context;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.flex.FlexDelegate;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import javax.inject.Inject;
import javax.inject.Singleton;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u0011\n\u0002\u0010\u0014\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0018\u0010\u0007\u001a\u00020\b2\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\t\u001a\u00020\nH\u0002J\u001f\u0010\u000b\u001a\u00020\f2\u0012\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u000e0\u000e\u00a2\u0006\u0002\u0010\u0010R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/example/nhatkyduonghuyet/ai/LSTMEngine;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "interpreter", "Lorg/tensorflow/lite/Interpreter;", "loadModel", "Ljava/nio/MappedByteBuffer;", "name", "", "predict", "", "input", "", "", "([[[F)F", "app_debug"})
public final class LSTMEngine {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final org.tensorflow.lite.Interpreter interpreter = null;
    
    public LSTMEngine(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    public final float predict(@org.jetbrains.annotations.NotNull()
    float[][][] input) {
        return 0.0F;
    }
    
    private final java.nio.MappedByteBuffer loadModel(android.content.Context context, java.lang.String name) {
        return null;
    }
}