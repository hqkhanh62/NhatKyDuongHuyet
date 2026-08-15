package com.example.nhatkyduonghuyet.ai;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u0014\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\u0004J\u000e\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0004J\u000e\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u000eJ\u001f\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\u00110\u00112\u0006\u0010\u000f\u001a\u00020\u000e\u00a2\u0006\u0002\u0010\u0012R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/example/nhatkyduonghuyet/ai/Normalizer;", "", "()V", "MAX_GLUCOSE_MMOL", "", "MIN_GLUCOSE_MMOL", "SEQUENCE_LENGTH", "", "denormalize", "normalizedValue", "isValidGlucose", "", "value", "normalize", "", "raw", "toLstmInput", "", "([F)[[[F", "app_debug"})
public final class Normalizer {
    public static final float MIN_GLUCOSE_MMOL = 2.0F;
    public static final float MAX_GLUCOSE_MMOL = 25.0F;
    public static final int SEQUENCE_LENGTH = 5;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.nhatkyduonghuyet.ai.Normalizer INSTANCE = null;
    
    private Normalizer() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final float[] normalize(@org.jetbrains.annotations.NotNull()
    float[] raw) {
        return null;
    }
    
    public final float denormalize(float normalizedValue) {
        return 0.0F;
    }
    
    public final boolean isValidGlucose(float value) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final float[][][] toLstmInput(@org.jetbrains.annotations.NotNull()
    float[] raw) {
        return null;
    }
}