package com.example.nhatkyduonghuyet.data.repository;

import android.content.Context;
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AIRepository_Factory implements Factory<AIRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<LogEntryDao> daoProvider;

  public AIRepository_Factory(Provider<Context> contextProvider,
      Provider<LogEntryDao> daoProvider) {
    this.contextProvider = contextProvider;
    this.daoProvider = daoProvider;
  }

  @Override
  public AIRepository get() {
    return newInstance(contextProvider.get(), daoProvider.get());
  }

  public static AIRepository_Factory create(Provider<Context> contextProvider,
      Provider<LogEntryDao> daoProvider) {
    return new AIRepository_Factory(contextProvider, daoProvider);
  }

  public static AIRepository newInstance(Context context, LogEntryDao dao) {
    return new AIRepository(context, dao);
  }
}
