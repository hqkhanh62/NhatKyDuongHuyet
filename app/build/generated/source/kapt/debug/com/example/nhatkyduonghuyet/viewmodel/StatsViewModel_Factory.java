package com.example.nhatkyduonghuyet.viewmodel;

import com.example.nhatkyduonghuyet.data.repository.AIRepository;
import com.example.nhatkyduonghuyet.data.repository.LogRepository;
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
public final class StatsViewModel_Factory implements Factory<StatsViewModel> {
  private final Provider<LogRepository> repoProvider;

  private final Provider<AIRepository> aiRepoProvider;

  public StatsViewModel_Factory(Provider<LogRepository> repoProvider,
      Provider<AIRepository> aiRepoProvider) {
    this.repoProvider = repoProvider;
    this.aiRepoProvider = aiRepoProvider;
  }

  @Override
  public StatsViewModel get() {
    return newInstance(repoProvider.get(), aiRepoProvider.get());
  }

  public static StatsViewModel_Factory create(Provider<LogRepository> repoProvider,
      Provider<AIRepository> aiRepoProvider) {
    return new StatsViewModel_Factory(repoProvider, aiRepoProvider);
  }

  public static StatsViewModel newInstance(LogRepository repo, AIRepository aiRepo) {
    return new StatsViewModel(repo, aiRepo);
  }
}
