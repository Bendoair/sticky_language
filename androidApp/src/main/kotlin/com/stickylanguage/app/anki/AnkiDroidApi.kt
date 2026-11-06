package com.stickylanguage.app.anki

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.stickylanguage.shared.anki.AnkiNoteType
import com.stickylanguage.shared.anki.AnkiPackageModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal client for AnkiDroid's instant-add ContentProvider (the `FlashCardsContract`).
 * Mirrors what `com.ichi2.anki.api.AddContentApi` does, without pulling the JitPack artifact.
 */
@Singleton
class AnkiDroidApi @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        const val PACKAGE = "com.ichi2.anki"
        const val PERMISSION = "com.ichi2.anki.permission.READ_WRITE_DATABASE"
        private const val AUTHORITY = "com.ichi2.anki.flashcards"
        private val NOTES_URI: Uri = Uri.parse("content://$AUTHORITY/notes")
        private val MODELS_URI: Uri = Uri.parse("content://$AUTHORITY/models")
        private val DECKS_URI: Uri = Uri.parse("content://$AUTHORITY/decks")
        private const val FIELD_SEP = "\u001f"
    }

    fun isInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(PACKAGE, 0); true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    data class AddResult(val added: Int, val failed: Int)

    /**
     * Adds notes to AnkiDroid. [notes] are (noteType, deckName, fields, tags) tuples.
     * Finds or creates the deck and the note type (model) by name.
     */
    suspend fun addNotes(notes: List<PreparedNote>): AddResult = withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        val deckIds = mutableMapOf<String, Long>()
        val modelIds = mutableMapOf<AnkiNoteType, Long>()
        var added = 0
        var failed = 0

        for (note in notes) {
            val deckId = deckIds.getOrPut(note.deckName) { findOrCreateDeck(note.deckName) }
            val modelId = modelIds.getOrPut(note.noteType) { findOrCreateModel(note.noteType, deckId) }
            val values = ContentValues().apply {
                put("mid", modelId)
                put("flds", note.fields.joinToString(FIELD_SEP))
                put("tags", note.tags.joinToString(" "))
            }
            val uri = cr.insert(NOTES_URI, values)
            if (uri == null) { failed++; continue }
            // Move the generated cards into the requested deck.
            val cardsUri = Uri.withAppendedPath(uri, "cards")
            cr.query(cardsUri, arrayOf("card_ord"), null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val ord = c.getString(0)
                    val cardUri = Uri.withAppendedPath(cardsUri, ord)
                    cr.update(cardUri, ContentValues().apply { put("deck_id", deckId) }, null, null)
                }
            }
            added++
        }
        AddResult(added, failed)
    }

    private fun findOrCreateDeck(name: String): Long {
        val cr = context.contentResolver
        cr.query(DECKS_URI, arrayOf("deck_id", "deck_name"), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1).equals(name, ignoreCase = true)) return c.getLong(0)
            }
        }
        val uri = cr.insert(DECKS_URI, ContentValues().apply { put("deck_name", name) })
            ?: error("AnkiDroid refused to create deck \"$name\"")
        return uri.lastPathSegment!!.toLong()
    }

    private fun findOrCreateModel(type: AnkiNoteType, deckId: Long): Long {
        val cr = context.contentResolver
        // Reuse an existing model with the same name and compatible field count.
        cr.query(MODELS_URI, arrayOf("_id", "name", "field_names"), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1)
                val fieldCount = c.getString(2).split(FIELD_SEP).size
                if (name == type.modelName && fieldCount >= type.fields.size) return c.getLong(0)
            }
        }
        val values = ContentValues().apply {
            put("name", type.modelName)
            put("field_names", type.fields.joinToString(FIELD_SEP))
            put("num_cards", 1)
            put("css", AnkiPackageModel.css)
            put("deck_id", deckId)
            put("sort_field_index", 0)
            put("type", 0)
        }
        val uri = cr.insert(MODELS_URI, values) ?: error("AnkiDroid refused to create note type \"${type.modelName}\"")
        val mid = uri.lastPathSegment!!.toLong()
        val tmplUri = Uri.withAppendedPath(Uri.withAppendedPath(MODELS_URI, mid.toString()), "templates/0")
        cr.update(
            tmplUri,
            ContentValues().apply {
                put("card_template_name", "Card 1")
                put("question_format", AnkiPackageModel.questionTemplate(type))
                put("answer_format", AnkiPackageModel.answerTemplate(type))
            },
            null, null,
        )
        return mid
    }
}

data class PreparedNote(
    val noteType: AnkiNoteType,
    val deckName: String,
    val fields: List<String>,
    val tags: List<String>,
)
