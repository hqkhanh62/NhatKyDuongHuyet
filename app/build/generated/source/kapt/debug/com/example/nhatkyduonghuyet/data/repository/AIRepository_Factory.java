package com.example.nhatkyduonghuyet.data.repository;

import android.content.Context;
import com.example.nhatkyduonghuyet.ai.LSTMEngine;
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

  private final Provider<LSTMEngine> modelProvider;

  public AIRepository_Factory(Provider<Context> contextProvider, Provider<LogEntryDao> daoProvider,
      Provider<LSTMEngine> modelProvider) {
    this.contextProvider = contextProvider;
    this.daoProvider = daoProvider;
    this.modelProvider = modelProvider;
  }

  @Override
  public AIRepository get() {
    return newInstance(contextProvider.get(), daoProvider.get(), modelProvider.get());
  }

  public static AIRepository_Factory create(Provider<Context> contextProvider,
      Provider<LogEntryDao> daoProvider, Provider<LSTMEngine> modelProvider) {
    return new AIRepository_Factory(contextProvider, daoProvider, modelProvider);
  }

  public static AIRepository newInstance(Context context, LogEntryDao dao, LSTMEngine model) {
    return new AIRepository(context, dao, model);
  }
}
