package com.example.nhatkyduonghuyet.viewmodel;

import com.example.nhatkyduonghuyet.domain.repository.LogRepository;
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
public final class LogEntryViewModel_Factory implements Factory<LogEntryViewModel> {
  private final Provider<LogRepository> repositoryProvider;

  public LogEntryViewModel_Factory(Provider<LogRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public LogEntryViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static LogEntryViewModel_Factory create(Provider<LogRepository> repositoryProvider) {
    return new LogEntryViewModel_Factory(repositoryProvider);
  }

  public static LogEntryViewModel newInstance(LogRepository repository) {
    return new LogEntryViewModel(repository);
  }
}
