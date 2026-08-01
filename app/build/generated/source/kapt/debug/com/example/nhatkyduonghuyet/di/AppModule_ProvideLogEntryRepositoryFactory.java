package com.example.nhatkyduonghuyet.di;

import com.example.nhatkyduonghuyet.data.LogEntryRepository;
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideLogEntryRepositoryFactory implements Factory<LogEntryRepository> {
  private final Provider<LogEntryDao> daoProvider;

  public AppModule_ProvideLogEntryRepositoryFactory(Provider<LogEntryDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public LogEntryRepository get() {
    return provideLogEntryRepository(daoProvider.get());
  }

  public static AppModule_ProvideLogEntryRepositoryFactory create(
      Provider<LogEntryDao> daoProvider) {
    return new AppModule_ProvideLogEntryRepositoryFactory(daoProvider);
  }

  public static LogEntryRepository provideLogEntryRepository(LogEntryDao dao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideLogEntryRepository(dao));
  }
}
