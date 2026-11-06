package com.stickylanguage.app.data.export

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.stickylanguage.shared.anki.AnkiNoteType
import com.stickylanguage.shared.anki.ExportNote
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "export_notes")
data class ExportNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val reading: String,
    val meaning: String,
    val grammar: String,
    val sentence: String,
    val sentenceTranslation: String,
    val languageCode: String,
    val deckOverride: String?,
    val noteTypeOverride: String?,
    val includeSentenceOverride: Boolean?,
    val createdAt: Long,
) {
    fun toDomain() = ExportNote(
        id = id,
        word = word,
        reading = reading,
        meaning = meaning,
        grammar = grammar,
        sentence = sentence,
        sentenceTranslation = sentenceTranslation,
        languageCode = languageCode,
        deckOverride = deckOverride,
        noteTypeOverride = noteTypeOverride?.let { AnkiNoteType.byName(it) },
        includeSentenceOverride = includeSentenceOverride,
        createdAt = createdAt,
    )

    companion object {
        fun from(n: ExportNote) = ExportNoteEntity(
            id = n.id,
            word = n.word,
            reading = n.reading,
            meaning = n.meaning,
            grammar = n.grammar,
            sentence = n.sentence,
            sentenceTranslation = n.sentenceTranslation,
            languageCode = n.languageCode,
            deckOverride = n.deckOverride,
            noteTypeOverride = n.noteTypeOverride?.name,
            includeSentenceOverride = n.includeSentenceOverride,
            createdAt = n.createdAt,
        )
    }
}

@Dao
interface ExportNoteDao {
    @Query("SELECT * FROM export_notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExportNoteEntity>>

    @Query("SELECT * FROM export_notes ORDER BY createdAt ASC")
    suspend fun getAll(): List<ExportNoteEntity>

    @Query("SELECT COUNT(*) FROM export_notes")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(note: ExportNoteEntity): Long

    @Insert
    suspend fun insertAll(notes: List<ExportNoteEntity>)

    @Update
    suspend fun update(note: ExportNoteEntity)

    @Delete
    suspend fun delete(note: ExportNoteEntity)

    @Query("DELETE FROM export_notes WHERE id IN (:ids)")
    suspend fun deleteIds(ids: List<Long>)

    @Query("DELETE FROM export_notes")
    suspend fun clear()
}

@Database(entities = [ExportNoteEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exportNoteDao(): ExportNoteDao
}
