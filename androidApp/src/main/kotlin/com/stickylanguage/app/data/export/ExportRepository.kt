package com.stickylanguage.app.data.export

import com.stickylanguage.shared.anki.ExportNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "export heap": flashcards waiting to be sent to Anki.
 */
@Singleton
class ExportRepository @Inject constructor(private val dao: ExportNoteDao) {

    val notes: Flow<List<ExportNote>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    val count: Flow<Int> = dao.observeCount()

    suspend fun all(): List<ExportNote> = dao.getAll().map { it.toDomain() }

    suspend fun add(note: ExportNote): Long = dao.insert(ExportNoteEntity.from(note.copy(id = 0)))

    suspend fun addAll(notes: List<ExportNote>) = dao.insertAll(notes.map { ExportNoteEntity.from(it.copy(id = 0)) })

    suspend fun update(note: ExportNote) = dao.update(ExportNoteEntity.from(note))

    suspend fun remove(note: ExportNote) = dao.delete(ExportNoteEntity.from(note))

    suspend fun removeIds(ids: List<Long>) = dao.deleteIds(ids)

    suspend fun clear() = dao.clear()
}
