package com.example.personaldiary

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DiaryDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "diary.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_ENTRIES = "entries"
        const val COLUMN_ID = "_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_IMAGE_PATH = "image_path"
        const val COLUMN_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_ENTRIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_CONTENT TEXT NOT NULL,
                $COLUMN_IMAGE_PATH TEXT,  -- Может быть NULL
                $COLUMN_CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ENTRIES")
        onCreate(db)
    }

    fun addEntry(title: String, content: String, imagePath: String?): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_IMAGE_PATH, imagePath)
        }
        return db.insert(TABLE_ENTRIES, null, values).also { db.close() }
    }

    fun getAllEntries() = readableDatabase.query(
        TABLE_ENTRIES,
        null,
        null,
        null,
        null,
        null,
        "$COLUMN_CREATED_AT DESC"
    )

    fun updateEntry(id: Long, title: String, content: String, imagePath: String?): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_IMAGE_PATH, imagePath)
        }
        return db.update(TABLE_ENTRIES, values, "$COLUMN_ID = ?", arrayOf(id.toString())).also { db.close() }
    }

    fun deleteEntry(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_ENTRIES, "$COLUMN_ID = ?", arrayOf(id.toString())).also { db.close() }
    }

    override fun close() {
        super.close()
    }
}