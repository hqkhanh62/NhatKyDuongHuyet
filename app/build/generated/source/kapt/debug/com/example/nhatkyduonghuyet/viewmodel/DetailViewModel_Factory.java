package com.example.nhatkyduonghuyet.viewmodel;

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
public final class DetailViewModel_Factory implements Factory<DetailViewModel> {
  private final Provider<LogRepository> repoProvider;

  public DetailViewModel_Factory(Provider<LogRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public DetailViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static DetailViewModel_Factory create(Provider<LogRepository> repoProvider) {
    return new DetailViewModel_Factory(repoProvider);
  }

  public static DetailViewModel newInstance(LogRepository repo) {
    return new DetailViewModel(repo);
  }
}
