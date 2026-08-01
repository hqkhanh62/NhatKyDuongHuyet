package com.example.nhatkyduonghuyet;

import com.example.nhatkyduonghuyet.ml.GlucosePredictor;
import com.example.nhatkyduonghuyet.ml.GlucoseScanner;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<GlucosePredictor> predictorProvider;

  private final Provider<GlucoseScanner> scannerProvider;

  public MainActivity_MembersInjector(Provider<GlucosePredictor> predictorProvider,
      Provider<GlucoseScanner> scannerProvider) {
    this.predictorProvider = predictorProvider;
    this.scannerProvider = scannerProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<GlucosePredictor> predictorProvider,
      Provider<GlucoseScanner> scannerProvider) {
    return new MainActivity_MembersInjector(predictorProvider, scannerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectPredictor(instance, predictorProvider.get());
    injectScanner(instance, scannerProvider.get());
  }

  @InjectedFieldSignature("com.example.nhatkyduonghuyet.MainActivity.predictor")
  public static void injectPredictor(MainActivity instance, GlucosePredictor predictor) {
    instance.predictor = predictor;
  }

  @InjectedFieldSignature("com.example.nhatkyduonghuyet.MainActivity.scanner")
  public static void injectScanner(MainActivity instance, GlucoseScanner scanner) {
    instance.scanner = scanner;
  }
}
