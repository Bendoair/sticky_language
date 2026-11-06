package com.stickylanguage.app.anki

import android.content.Context
import android.net.Uri
import com.stickylanguage.app.data.export.ExportRepository
import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.shared.anki.AnkiDefaults
import com.stickylanguage.shared.anki.AnkiFieldMapper
import com.stickylanguage.shared.anki.ExportNote
import com.stickylanguage.shared.anki.ExportTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** What the Export screen must do for "Export all" given the current settings and device. */
sealed interface ExportPlan {
    data object Nothing : ExportPlan
    data object NeedsAnkiDroidPermission : ExportPlan
    data object AnkiDroid : ExportPlan
    data class ApkgFile(val suggestedName: String) : ExportPlan
}

sealed interface ExportOutcome {
    data class Success(val count: Int, val target: String) : ExportOutcome
    data class Failure(val message: String) : ExportOutcome
}

@Singleton
class AnkiExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ankiDroid: AnkiDroidApi,
    private val apkgWriter: ApkgWriter,
    private val exportRepository: ExportRepository,
    private val settings: SettingsRepository,
) {
    fun ankiDroidInstalled(): Boolean = ankiDroid.isInstalled()

    suspend fun plan(): ExportPlan {
        val notes = exportRepository.all()
        if (notes.isEmpty()) return ExportPlan.Nothing
        val target = settings.current().anki.exportTarget
        val useAnkiDroid = when (target) {
            ExportTarget.AUTO -> ankiDroid.isInstalled()
            ExportTarget.ANKIDROID -> ankiDroid.isInstalled()
            ExportTarget.APKG_FILE -> false
        }
        return when {
            useAnkiDroid && !ankiDroid.hasPermission() -> ExportPlan.NeedsAnkiDroidPermission
            useAnkiDroid -> ExportPlan.AnkiDroid
            else -> ExportPlan.ApkgFile(suggestedFileName())
        }
    }

    suspend fun exportToAnkiDroid(): ExportOutcome {
        val notes = exportRepository.all()
        if (notes.isEmpty()) return ExportOutcome.Failure("Nothing to export")
        val defaults = settings.current().anki
        return try {
            val prepared = notes.map { it.prepare(defaults) }
            val result = ankiDroid.addNotes(prepared)
            if (result.added > 0) {
                exportRepository.removeIds(notes.take(result.added).map { it.id })
            }
            if (result.failed == 0) ExportOutcome.Success(result.added, "AnkiDroid")
            else ExportOutcome.Failure("Added ${result.added}, failed ${result.failed}")
        } catch (e: Exception) {
            ExportOutcome.Failure(e.message ?: "AnkiDroid export failed")
        }
    }

    suspend fun exportToFile(uri: Uri): ExportOutcome {
        val notes = exportRepository.all()
        if (notes.isEmpty()) return ExportOutcome.Failure("Nothing to export")
        val defaults = settings.current().anki
        return try {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")?.use { apkgWriter.write(notes, defaults, it) }
                    ?: error("Could not open the destination file")
            }
            exportRepository.removeIds(notes.map { it.id })
            ExportOutcome.Success(notes.size, ".apkg file")
        } catch (e: Exception) {
            ExportOutcome.Failure(e.message ?: "File export failed")
        }
    }

    private fun suggestedFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        return "sticky-language-$stamp.apkg"
    }

    private fun ExportNote.prepare(defaults: AnkiDefaults) = PreparedNote(
        noteType = noteType(defaults),
        deckName = deck(defaults),
        fields = AnkiFieldMapper.fields(this, defaults),
        tags = (defaults.tags + languageCode.takeIf { it.isNotBlank() }.let { if (it == null) emptyList() else listOf(it) }).distinct(),
    )
}
