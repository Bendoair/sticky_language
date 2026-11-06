package com.stickylanguage.shared.anki

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.security.MessageDigest
import kotlin.random.Random

/**
 * Platform-independent description of a legacy (schema v11) Anki collection, which is what an
 * `.apkg` contains. The Android layer writes the SQLite file; this class produces the JSON blobs
 * for the `col` row and the per-note rows.
 */
object AnkiPackageModel {

    const val SCHEMA_VERSION = 11
    private const val FIELD_SEPARATOR = '\u001f'

    /** Stable model ids so re-imports update instead of duplicating note types. */
    fun modelId(type: AnkiNoteType): Long = when (type) {
        AnkiNoteType.BASIC -> 1_607_392_319_001L
        AnkiNoteType.STICKY -> 1_607_392_319_002L
    }

    data class NoteRow(
        val id: Long,
        val guid: String,
        val modelId: Long,
        val deckId: Long,
        val fields: String,
        val sortField: String,
        val checksum: Long,
        val tags: String,
    )

    data class Package(
        val created: Long,
        val conf: String,
        val models: String,
        val decks: String,
        val dconf: String,
        val notes: List<NoteRow>,
    )

    fun build(notes: List<ExportNote>, defaults: AnkiDefaults, now: Long = System.currentTimeMillis()): Package {
        val nowSec = now / 1000
        val deckNames = notes.map { it.deck(defaults) }.distinct()
        val deckIds = deckNames.withIndex().associate { (i, name) -> name to (now + i + 1) }
        val usedTypes = notes.map { it.noteType(defaults) }.distinct()

        val rows = notes.mapIndexed { i, note ->
            val type = note.noteType(defaults)
            val fields = AnkiFieldMapper.fields(note, defaults)
            val sort = stripHtml(fields.first())
            NoteRow(
                id = now + i,
                guid = guid(),
                modelId = modelId(type),
                deckId = deckIds.getValue(note.deck(defaults)),
                fields = fields.joinToString(FIELD_SEPARATOR.toString()),
                sortField = sort,
                checksum = checksum(sort),
                tags = " " + (defaults.tags + "sticky-language").distinct().joinToString(" ") + " ",
            )
        }

        return Package(
            created = nowSec,
            conf = conf(deckIds.values.first()).toString(),
            models = models(usedTypes, deckIds.values.first(), nowSec).toString(),
            decks = decks(deckIds, nowSec).toString(),
            dconf = dconf().toString(),
            notes = rows,
        )
    }

    private fun conf(curDeck: Long) = buildJsonObject {
        putJsonArray("activeDecks") { add(curDeck) }
        put("curDeck", curDeck)
        put("newSpread", 0)
        put("collapseTime", 1200)
        put("timeLim", 0)
        put("estTimes", true)
        put("dueCounts", true)
        put("curModel", modelId(AnkiNoteType.STICKY).toString())
        put("nextPos", 1)
        put("sortType", "noteFld")
        put("sortBackwards", false)
        put("addToCur", true)
    }

    private fun decks(deckIds: Map<String, Long>, nowSec: Long): JsonObject = buildJsonObject {
        put("1", deck(1, "Default", nowSec))
        deckIds.forEach { (name, id) -> put(id.toString(), deck(id, name, nowSec)) }
    }

    private fun deck(id: Long, name: String, nowSec: Long) = buildJsonObject {
        put("id", id)
        put("name", name)
        put("mod", nowSec)
        put("usn", -1)
        putJsonArray("lrnToday") { add(0); add(0) }
        putJsonArray("revToday") { add(0); add(0) }
        putJsonArray("newToday") { add(0); add(0) }
        putJsonArray("timeToday") { add(0); add(0) }
        put("collapsed", false)
        put("browserCollapsed", false)
        put("desc", "")
        put("dyn", 0)
        put("conf", 1)
        put("extendNew", 0)
        put("extendRev", 0)
    }

    private fun dconf() = buildJsonObject {
        putJsonObject("1") {
            put("id", 1)
            put("name", "Default")
            put("replayq", true)
            putJsonObject("lapse") {
                put("leechFails", 8); put("minInt", 1); putJsonArray("delays") { add(10) }; put("leechAction", 0); put("mult", 0)
            }
            putJsonObject("rev") {
                put("perDay", 100); put("fuzz", 0.05); put("ivlFct", 1); put("maxIvl", 36500); put("ease4", 1.3); put("bury", true); put("minSpace", 1)
            }
            put("timer", 0)
            put("maxTaken", 60)
            put("usn", 0)
            putJsonObject("new") {
                put("perDay", 20); putJsonArray("delays") { add(1); add(10) }; put("separate", true)
                putJsonArray("ints") { add(1); add(4); add(7) }; put("initialFactor", 2500); put("bury", true); put("order", 1)
            }
            put("mod", 0)
            put("autoplay", true)
        }
    }

    private fun models(types: List<AnkiNoteType>, deckId: Long, nowSec: Long): JsonObject = buildJsonObject {
        types.forEach { type -> put(modelId(type).toString(), model(type, deckId, nowSec)) }
    }

    fun questionTemplate(type: AnkiNoteType): String = when (type) {
        AnkiNoteType.BASIC -> "{{Front}}"
        AnkiNoteType.STICKY -> "<div class=\"word\">{{Word}}</div>"
    }

    fun answerTemplate(type: AnkiNoteType): String = when (type) {
        AnkiNoteType.BASIC -> "{{FrontSide}}<hr id=answer>{{Back}}"
        AnkiNoteType.STICKY -> """
            {{FrontSide}}
            <hr id=answer>
            {{#Reading}}<div class="reading">{{Reading}}</div>{{/Reading}}
            <div class="meaning">{{Meaning}}</div>
            {{#Grammar}}<div class="grammar">{{Grammar}}</div>{{/Grammar}}
            {{#Sentence}}<div class="sentence">{{Sentence}}</div>{{/Sentence}}
        """.trimIndent()
    }

    val css: String = """
        .card { font-family: sans-serif; font-size: 22px; text-align: center; color: #222; background: #fffdf5; }
        .word { font-size: 40px; }
        .reading { color: #777; font-size: 18px; }
        .meaning { margin-top: 8px; }
        .grammar { margin-top: 10px; font-size: 16px; color: #555; }
        .sentence { margin-top: 14px; font-size: 18px; border-top: 1px dashed #ccc; padding-top: 10px; }
    """.trimIndent()

    private fun model(type: AnkiNoteType, deckId: Long, nowSec: Long) = buildJsonObject {
        put("id", modelId(type))
        put("name", type.modelName)
        put("type", 0)
        put("mod", nowSec)
        put("usn", -1)
        put("sortf", 0)
        put("did", deckId)
        putJsonArray("tmpls") {
            add(buildJsonObject {
                put("name", "Card 1")
                put("ord", 0)
                put("qfmt", questionTemplate(type))
                put("afmt", answerTemplate(type))
                put("bqfmt", "")
                put("bafmt", "")
                put("did", null as String?)
                put("bfont", "")
                put("bsize", 0)
            })
        }
        putJsonArray("flds") {
            type.fields.forEachIndexed { i, name ->
                add(buildJsonObject {
                    put("name", name)
                    put("ord", i)
                    put("sticky", false)
                    put("rtl", false)
                    put("font", "Arial")
                    put("size", 20)
                    putJsonArray("media") { }
                })
            }
        }
        put("css", css)
        put("latexPre", "\\documentclass[12pt]{article}\n\\special{papersize=3in,5in}\n\\usepackage[utf8]{inputenc}\n\\usepackage{amssymb,amsmath}\n\\pagestyle{empty}\n\\setlength{\\parindent}{0in}\n\\begin{document}\n")
        put("latexPost", "\\end{document}")
        put("latexsvg", false)
        putJsonArray("req") {
            add(buildJsonArray { add(0); add("all"); add(buildJsonArray { add(0) }) })
        }
        putJsonArray("tags") { }
        putJsonArray("vers") { }
    }

    private const val GUID_CHARS =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$%&()*+,-./:;<=>?@[]^_`{|}~"

    fun guid(random: Random = Random.Default): String {
        var n = random.nextLong().let { if (it < 0) -(it + 1) else it }
        val sb = StringBuilder()
        val base = GUID_CHARS.length
        do {
            sb.append(GUID_CHARS[(n % base).toInt()])
            n /= base
        } while (n > 0)
        return sb.toString()
    }

    /** Anki checksum: first 8 hex digits of SHA-1 of the stripped sort field. */
    fun checksum(sortField: String): Long {
        val digest = MessageDigest.getInstance("SHA-1").digest(sortField.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return hex.take(8).toLong(16)
    }

    fun stripHtml(s: String): String = s.replace(Regex("<[^>]+>"), "")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
}
