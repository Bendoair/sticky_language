package com.stickylanguage.app.di

import android.content.Context
import androidx.room.Room
import com.stickylanguage.app.data.export.AppDatabase
import com.stickylanguage.app.data.export.ExportNoteDao
import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.app.llm.RoutingLlmBackend
import com.stickylanguage.shared.llm.LlmBackend
import com.stickylanguage.shared.llm.RemoteLlmBackend
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "sticky-language.db").build()

    @Provides
    fun provideExportNoteDao(db: AppDatabase): ExportNoteDao = db.exportNoteDao()

    @Provides
    @Singleton
    fun provideHttpEngine(): HttpClientEngine = OkHttp.create()

    @Provides
    @Singleton
    fun provideRemoteLlmBackend(engine: HttpClientEngine, settings: SettingsRepository): RemoteLlmBackend =
        RemoteLlmBackend(engine) { settings.current().llmConfig }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmBindingsModule {
    @Binds
    @Singleton
    abstract fun bindLlmBackend(impl: RoutingLlmBackend): LlmBackend
}
