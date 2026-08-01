package com.example.nhatkyduonghuyet.data;

import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class LogEntryRepository_Factory implements Factory<LogEntryRepository> {
  private final Provider<LogEntryDao> logEntryDaoProvider;

  public LogEntryRepository_Factory(Provider<LogEntryDao> logEntryDaoProvider) {
    this.logEntryDaoProvider = logEntryDaoProvider;
  }

  @Override
  public LogEntryRepository get() {
    return newInstance(logEntryDaoProvider.get());
  }

  public static LogEntryRepository_Factory create(Provider<LogEntryDao> logEntryDaoProvider) {
    return new LogEntryRepository_Factory(logEntryDaoProvider);
  }

  public static LogEntryRepository newInstance(LogEntryDao logEntryDao) {
    return new LogEntryRepository(logEntryDao);
  }
}
