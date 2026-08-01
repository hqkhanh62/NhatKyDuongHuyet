package com.example.nhatkyduonghuyet.ml;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class GlucoseScanner_Factory implements Factory<GlucoseScanner> {
  @Override
  public GlucoseScanner get() {
    return newInstance();
  }

  public static GlucoseScanner_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GlucoseScanner newInstance() {
    return new GlucoseScanner();
  }

  private static final class InstanceHolder {
    private static final GlucoseScanner_Factory INSTANCE = new GlucoseScanner_Factory();
  }
}
