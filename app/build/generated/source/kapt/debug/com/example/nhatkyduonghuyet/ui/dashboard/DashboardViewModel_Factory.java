package com.example.nhatkyduonghuyet.ui.dashboard;

import com.example.nhatkyduonghuyet.ai.RealtimePredictor;
import com.example.nhatkyduonghuyet.data.repository.AIRepository;
import com.example.nhatkyduonghuyet.data.repository.LogRepository;
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern;
import com.example.nhatkyduonghuyet.ml.GlucosePredictor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<LogRepository> repoProvider;

  private final Provider<GlucosePredictor> predictorProvider;

  private final Provider<RealtimePredictor> realtimePredictorProvider;

  private final Provider<DetectRiskPattern> detectRiskProvider;

  private final Provider<AIRepository> aiRepoProvider;

  public DashboardViewModel_Factory(Provider<LogRepository> repoProvider,
      Provider<GlucosePredictor> predictorProvider,
      Provider<RealtimePredictor> realtimePredictorProvider,
      Provider<DetectRiskPattern> detectRiskProvider, Provider<AIRepository> aiRepoProvider) {
    this.repoProvider = repoProvider;
    this.predictorProvider = predictorProvider;
    this.realtimePredictorProvider = realtimePredictorProvider;
    this.detectRiskProvider = detectRiskProvider;
    this.aiRepoProvider = aiRepoProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(repoProvider.get(), predictorProvider.get(), realtimePredictorProvider.get(), detectRiskProvider.get(), aiRepoProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<LogRepository> repoProvider,
      Provider<GlucosePredictor> predictorProvider,
      Provider<RealtimePredictor> realtimePredictorProvider,
      Provider<DetectRiskPattern> detectRiskProvider, Provider<AIRepository> aiRepoProvider) {
    return new DashboardViewModel_Factory(repoProvider, predictorProvider, realtimePredictorProvider, detectRiskProvider, aiRepoProvider);
  }

  public static DashboardViewModel newInstance(LogRepository repo, GlucosePredictor predictor,
      RealtimePredictor realtimePredictor, DetectRiskPattern detectRisk, AIRepository aiRepo) {
    return new DashboardViewModel(repo, predictor, realtimePredictor, detectRisk, aiRepo);
  }
}
