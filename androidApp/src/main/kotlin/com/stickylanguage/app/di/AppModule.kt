package com.stickylanguage.app.di

import android.content.Context
import androidx.room.Room
import com.stickylanguage.app.data.export.AppDatabase
import com.stickylanguage.app.data.export.ExportNoteDao
import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.shared.llm.LlmClient
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
    fun provideLlmClient(engine: HttpClientEngine, settings: SettingsRepository): LlmClient =
        LlmClient(engine) { settings.current().llmConfig }
}
