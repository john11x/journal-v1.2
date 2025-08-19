package com.example.journalapp.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.journalapp.models.JournalEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class JournalDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "journal.db"
        private const val DATABASE_VERSION = 4

        private const val TABLE_ENTRIES = "entries"
        private const val COLUMN_ID = "id"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_MOOD = "mood"
        private const val COLUMN_TAGS = "tags"
        private const val COLUMN_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_ENTRIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CONTENT TEXT NOT NULL,
                $COLUMN_MOOD INTEGER DEFAULT 0,
                $COLUMN_TAGS TEXT,
                $COLUMN_DATE INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_ENTRIES ADD COLUMN $COLUMN_TAGS TEXT DEFAULT ''")
        }
    }

    fun addOrUpdateEntry(entry: JournalEntry): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_CONTENT, entry.content)
            put(COLUMN_MOOD, entry.mood)
            put(COLUMN_TAGS, entry.tags)
            put(COLUMN_DATE, entry.date)
        }

        return if (entry.id > 0) {
            // Update existing entry
            writableDatabase.update(
                TABLE_ENTRIES,
                values,
                "$COLUMN_ID = ?",
                arrayOf(entry.id.toString())
            ) > 0
        } else {
            // Always create new entry
            writableDatabase.insert(TABLE_ENTRIES, null, values) != -1L
        }
    }

    fun getEntriesByDate(date: Long): List<JournalEntry> {
        val cursor = readableDatabase.query(
            TABLE_ENTRIES,
            arrayOf(COLUMN_ID, COLUMN_CONTENT, COLUMN_MOOD, COLUMN_TAGS, COLUMN_DATE),
            "$COLUMN_DATE BETWEEN ? AND ?",
            arrayOf(getStartOfDay(date).toString(), getEndOfDay(date).toString()),
            null, null, "$COLUMN_DATE DESC"
        )
        return cursor.parseJournalEntries()
    }


    fun getEntryByDateRange(startDate: Long, endDate: Long): JournalEntry? {
        val cursor = readableDatabase.query(
            TABLE_ENTRIES,
            arrayOf(COLUMN_ID, COLUMN_CONTENT, COLUMN_MOOD, COLUMN_TAGS, COLUMN_DATE),
            "$COLUMN_DATE BETWEEN ? AND ?",
            arrayOf(startDate.toString(), endDate.toString()),
            null, null, null
        )
        return cursor.parseJournalEntry()
    }

    fun getAllEntries(): List<JournalEntry> {
        val cursor = readableDatabase.query(
            TABLE_ENTRIES,
            arrayOf(COLUMN_ID, COLUMN_CONTENT, COLUMN_MOOD, COLUMN_TAGS, COLUMN_DATE),
            null, null, null, null,
            "$COLUMN_DATE DESC"
        )
        return cursor.parseJournalEntries()
    }

    private fun Cursor.parseJournalEntry(): JournalEntry? {
        return if (moveToFirst()) {
            JournalEntry(
                id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                content = getString(getColumnIndexOrThrow(COLUMN_CONTENT)),
                mood = getInt(getColumnIndexOrThrow(COLUMN_MOOD)),
                tags = getString(getColumnIndexOrThrow(COLUMN_TAGS)),
                date = getLong(getColumnIndexOrThrow(COLUMN_DATE))
            )
        } else {
            null
        }.also { close() }
    }

    private fun Cursor.parseJournalEntries(): List<JournalEntry> {
        val entries = mutableListOf<JournalEntry>()
        while (moveToNext()) {
            entries.add(JournalEntry(
                id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                content = getString(getColumnIndexOrThrow(COLUMN_CONTENT)),
                mood = getInt(getColumnIndexOrThrow(COLUMN_MOOD)),
                tags = getString(getColumnIndexOrThrow(COLUMN_TAGS)),
                date = getLong(getColumnIndexOrThrow(COLUMN_DATE))
            ))
        }
        close()
        return entries
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    fun deleteEntry(id: Long): Boolean {
        return writableDatabase.delete(
            TABLE_ENTRIES,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        ) > 0
    }

    private fun getEndOfDay(timestamp: Long): Long {
        return getStartOfDay(timestamp) + 86400000
    }
    fun searchEntries(query: String): List<JournalEntry> {
        val searchQuery = "%$query%"

        // Try to parse as date if query looks like a date (e.g., "Aug 3")
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val dateQuery = try {
            val date = dateFormat.parse(query)
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            null
        }

        return readableDatabase.query(
            TABLE_ENTRIES,
            arrayOf(COLUMN_ID, COLUMN_CONTENT, COLUMN_MOOD, COLUMN_TAGS, COLUMN_DATE),
            "$COLUMN_CONTENT LIKE ? OR $COLUMN_TAGS LIKE ? OR $COLUMN_DATE LIKE ?",
            arrayOf(searchQuery, searchQuery, dateQuery ?: searchQuery),
            null, null,
            "$COLUMN_DATE DESC"
        ).parseJournalEntries()
    }
}