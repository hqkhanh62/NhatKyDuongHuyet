package com.example.nhatkyduonghuyet.di;

import com.example.nhatkyduonghuyet.data.local.AppDatabase;
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideDaoFactory implements Factory<LogEntryDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public LogEntryDao get() {
    return provideDao(dbProvider.get());
  }

  public static AppModule_ProvideDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideDaoFactory(dbProvider);
  }

  public static LogEntryDao provideDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDao(db));
  }
}
