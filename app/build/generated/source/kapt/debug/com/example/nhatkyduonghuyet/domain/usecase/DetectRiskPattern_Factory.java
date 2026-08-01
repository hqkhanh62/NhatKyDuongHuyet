package com.example.nhatkyduonghuyet.domain.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class DetectRiskPattern_Factory implements Factory<DetectRiskPattern> {
  @Override
  public DetectRiskPattern get() {
    return newInstance();
  }

  public static DetectRiskPattern_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DetectRiskPattern newInstance() {
    return new DetectRiskPattern();
  }

  private static final class InstanceHolder {
    private static final DetectRiskPattern_Factory INSTANCE = new DetectRiskPattern_Factory();
  }
}
