package com.example.nhatkyduonghuyet.ai;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class RealtimePredictor_Factory implements Factory<RealtimePredictor> {
  private final Provider<LSTMEngine> modelProvider;

  public RealtimePredictor_Factory(Provider<LSTMEngine> modelProvider) {
    this.modelProvider = modelProvider;
  }

  @Override
  public RealtimePredictor get() {
    return newInstance(modelProvider.get());
  }

  public static RealtimePredictor_Factory create(Provider<LSTMEngine> modelProvider) {
    return new RealtimePredictor_Factory(modelProvider);
  }

  public static RealtimePredictor newInstance(LSTMEngine model) {
    return new RealtimePredictor(model);
  }
}
