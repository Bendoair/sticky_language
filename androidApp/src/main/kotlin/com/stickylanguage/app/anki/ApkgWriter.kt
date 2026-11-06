package com.stickylanguage.app.anki

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.stickylanguage.shared.anki.AnkiDefaults
import com.stickylanguage.shared.anki.AnkiPackageModel
import com.stickylanguage.shared.anki.ExportNote
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes a legacy-format `.apkg` (zip with `collection.anki2` SQLite + `media` JSON),
 * importable by Anki desktop, AnkiMobile and AnkiDroid.
 */
@Singleton
class ApkgWriter @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun write(notes: List<ExportNote>, defaults: AnkiDefaults, out: OutputStream) = withContext(Dispatchers.IO) {
        val pkg = AnkiPackageModel.build(notes, defaults)
        val dbFile = File.createTempFile("collection", ".anki2", context.cacheDir)
        dbFile.delete()
        try {
            SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db -> populate(db, pkg) }
            ZipOutputStream(out.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("collection.anki2"))
                FileInputStream(dbFile).use { it.copyTo(zip) }
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("media"))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }
        } finally {
            dbFile.delete()
            File(dbFile.path + "-journal").delete()
        }
    }

    private fun populate(db: SQLiteDatabase, pkg: AnkiPackageModel.Package) {
        db.beginTransaction()
        try {
            SCHEMA.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { db.execSQL(it) }
            db.execSQL(
                "INSERT INTO col VALUES (1, ?, ?, ?, ?, 0, 0, 0, ?, ?, ?, ?, '{}')",
                arrayOf(pkg.created, pkg.created * 1000, pkg.created * 1000, AnkiPackageModel.SCHEMA_VERSION,
                    pkg.conf, pkg.models, pkg.decks, pkg.dconf),
            )
            val noteStmt = db.compileStatement(
                "INSERT INTO notes (id, guid, mid, mod, usn, tags, flds, sfld, csum, flags, data) VALUES (?,?,?,?,?,?,?,?,?,0,'')",
            )
            val cardStmt = db.compileStatement(
                "INSERT INTO cards (id, nid, did, ord, mod, usn, type, queue, due, ivl, factor, reps, lapses, left, odue, odid, flags, data) " +
                    "VALUES (?,?,?,0,?,-1,0,0,?,0,0,0,0,0,0,0,0,'')",
            )
            pkg.notes.forEachIndexed { i, n ->
                noteStmt.clearBindings()
                noteStmt.bindLong(1, n.id)
                noteStmt.bindString(2, n.guid)
                noteStmt.bindLong(3, n.modelId)
                noteStmt.bindLong(4, pkg.created)
                noteStmt.bindLong(5, -1)
                noteStmt.bindString(6, n.tags)
                noteStmt.bindString(7, n.fields)
                noteStmt.bindString(8, n.sortField)
                noteStmt.bindLong(9, n.checksum)
                noteStmt.executeInsert()

                cardStmt.clearBindings()
                cardStmt.bindLong(1, n.id + 1_000_000)
                cardStmt.bindLong(2, n.id)
                cardStmt.bindLong(3, n.deckId)
                cardStmt.bindLong(4, pkg.created)
                cardStmt.bindLong(5, (i + 1).toLong())
                cardStmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private companion object {
        val SCHEMA = """
            CREATE TABLE col (
              id integer primary key, crt integer not null, mod integer not null, scm integer not null,
              ver integer not null, dty integer not null, usn integer not null, ls integer not null,
              conf text not null, models text not null, decks text not null, dconf text not null, tags text not null
            );
            CREATE TABLE notes (
              id integer primary key, guid text not null, mid integer not null, mod integer not null,
              usn integer not null, tags text not null, flds text not null, sfld integer not null,
              csum integer not null, flags integer not null, data text not null
            );
            CREATE TABLE cards (
              id integer primary key, nid integer not null, did integer not null, ord integer not null,
              mod integer not null, usn integer not null, type integer not null, queue integer not null,
              due integer not null, ivl integer not null, factor integer not null, reps integer not null,
              lapses integer not null, left integer not null, odue integer not null, odid integer not null,
              flags integer not null, data text not null
            );
            CREATE TABLE revlog (
              id integer primary key, cid integer not null, usn integer not null, ease integer not null,
              ivl integer not null, lastIvl integer not null, factor integer not null, time integer not null,
              type integer not null
            );
            CREATE TABLE graves (usn integer not null, oid integer not null, type integer not null);
            CREATE INDEX ix_notes_usn on notes (usn);
            CREATE INDEX ix_cards_usn on cards (usn);
            CREATE INDEX ix_revlog_usn on revlog (usn);
            CREATE INDEX ix_cards_nid on cards (nid);
            CREATE INDEX ix_cards_sched on cards (did, queue, due);
            CREATE INDEX ix_revlog_cid on revlog (cid);
            CREATE INDEX ix_notes_csum on notes (csum)
        """.trimIndent()
    }
}
