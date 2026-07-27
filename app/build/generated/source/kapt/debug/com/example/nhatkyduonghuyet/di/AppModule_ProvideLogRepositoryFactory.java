package com.example.nhatkyduonghuyet.di;

import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao;
import com.example.nhatkyduonghuyet.data.repository.LogRepository;
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
public final class AppModule_ProvideLogRepositoryFactory implements Factory<LogRepository> {
  private final Provider<LogEntryDao> daoProvider;

  public AppModule_ProvideLogRepositoryFactory(Provider<LogEntryDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public LogRepository get() {
    return provideLogRepository(daoProvider.get());
  }

  public static AppModule_ProvideLogRepositoryFactory create(Provider<LogEntryDao> daoProvider) {
    return new AppModule_ProvideLogRepositoryFactory(daoProvider);
  }

  public static LogRepository provideLogRepository(LogEntryDao dao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideLogRepository(dao));
  }
}
