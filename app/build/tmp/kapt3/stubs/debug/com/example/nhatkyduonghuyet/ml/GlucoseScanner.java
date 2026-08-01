package com.example.nhatkyduonghuyet.ml;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0005\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0007\u001a\u00020\u0006H\u0002J\u0017\u0010\b\u001a\u0004\u0018\u00010\t2\u0006\u0010\u0007\u001a\u00020\u0006H\u0002\u00a2\u0006\u0002\u0010\nJ\u0012\u0010\u000b\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0007\u001a\u00020\u0006H\u0002JV\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\r0\u00112\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\r0\u00142\u0016\u0010\u0015\u001a\u0012\u0012\b\u0012\u00060\u0016j\u0002`\u0017\u0012\u0004\u0012\u00020\r0\u00112\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\r0\u0014R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0019"}, d2 = {"Lcom/example/nhatkyduonghuyet/ml/GlucoseScanner;", "", "()V", "recognizer", "Lcom/google/mlkit/vision/text/TextRecognizer;", "extractDate", "", "text", "extractGlucose", "", "(Ljava/lang/String;)Ljava/lang/Float;", "extractTime", "processImage", "", "image", "Lcom/google/mlkit/vision/common/InputImage;", "onSuccess", "Lkotlin/Function1;", "Lcom/example/nhatkyduonghuyet/ml/ScannedGlucoseResult;", "onNoResult", "Lkotlin/Function0;", "onError", "Ljava/lang/Exception;", "Lkotlin/Exception;", "onComplete", "app_debug"})
public final class GlucoseScanner {
    @org.jetbrains.annotations.NotNull()
    private final com.google.mlkit.vision.text.TextRecognizer recognizer = null;
    
    @javax.inject.Inject()
    public GlucoseScanner() {
        super();
    }
    
    public final void processImage(@org.jetbrains.annotations.NotNull()
    com.google.mlkit.vision.common.InputImage image, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult, kotlin.Unit> onSuccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNoResult, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Exception, kotlin.Unit> onError, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onComplete) {
    }
    
    private final java.lang.Float extractGlucose(java.lang.String text) {
        return null;
    }
    
    private final java.lang.String extractTime(java.lang.String text) {
        return null;
    }
    
    private final java.lang.String extractDate(java.lang.String text) {
        return null;
    }
}