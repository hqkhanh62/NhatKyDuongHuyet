package com.example.nhatkyduonghuyet.ui.dashboard;

import com.example.nhatkyduonghuyet.ai.RealtimePredictor;
import com.example.nhatkyduonghuyet.data.repository.AIRepository;
import com.example.nhatkyduonghuyet.domain.repository.LogRepository;
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern;
import com.example.nhatkyduonghuyet.domain.usecase.GeminiAnalysisUseCase;
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

  private final Provider<RealtimePredictor> realtimePredictorProvider;

  private final Provider<DetectRiskPattern> detectRiskProvider;

  private final Provider<AIRepository> aiRepoProvider;

  private final Provider<GeminiAnalysisUseCase> geminiUseCaseProvider;

  public DashboardViewModel_Factory(Provider<LogRepository> repoProvider,
      Provider<RealtimePredictor> realtimePredictorProvider,
      Provider<DetectRiskPattern> detectRiskProvider, Provider<AIRepository> aiRepoProvider,
      Provider<GeminiAnalysisUseCase> geminiUseCaseProvider) {
    this.repoProvider = repoProvider;
    this.realtimePredictorProvider = realtimePredictorProvider;
    this.detectRiskProvider = detectRiskProvider;
    this.aiRepoProvider = aiRepoProvider;
    this.geminiUseCaseProvider = geminiUseCaseProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(repoProvider.get(), realtimePredictorProvider.get(), detectRiskProvider.get(), aiRepoProvider.get(), geminiUseCaseProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<LogRepository> repoProvider,
      Provider<RealtimePredictor> realtimePredictorProvider,
      Provider<DetectRiskPattern> detectRiskProvider, Provider<AIRepository> aiRepoProvider,
      Provider<GeminiAnalysisUseCase> geminiUseCaseProvider) {
    return new DashboardViewModel_Factory(repoProvider, realtimePredictorProvider, detectRiskProvider, aiRepoProvider, geminiUseCaseProvider);
  }

  public static DashboardViewModel newInstance(LogRepository repo,
      RealtimePredictor realtimePredictor, DetectRiskPattern detectRisk, AIRepository aiRepo,
      GeminiAnalysisUseCase geminiUseCase) {
    return new DashboardViewModel(repo, realtimePredictor, detectRisk, aiRepo, geminiUseCase);
  }
}
