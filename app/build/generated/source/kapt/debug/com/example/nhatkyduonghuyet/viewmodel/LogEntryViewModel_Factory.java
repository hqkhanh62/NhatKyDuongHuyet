package com.example.nhatkyduonghuyet.viewmodel;

import com.example.nhatkyduonghuyet.data.LogEntryRepository;
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
  private final Provider<LogEntryRepository> repositoryProvider;

  public LogEntryViewModel_Factory(Provider<LogEntryRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public LogEntryViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static LogEntryViewModel_Factory create(Provider<LogEntryRepository> repositoryProvider) {
    return new LogEntryViewModel_Factory(repositoryProvider);
  }

  public static LogEntryViewModel newInstance(LogEntryRepository repository) {
    return new LogEntryViewModel(repository);
  }
}
