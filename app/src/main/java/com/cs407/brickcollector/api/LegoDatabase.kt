package com.cs407.brickcollector.api

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream

data class LegoSet(
    val setId: Int,
    val setNumber: String,
    val name: String,
    val theme: String,
    val subtheme: String?,
    val year: Int,
    val pieces: Int,
    val usedPrice: Double?,
    val newPrice: Double?,
    val upc: String?,
    val itemNumberNA: String?,
    val imageUrl: String?,
    val thumbnailUrl: String?
)

class LegoDatabase private constructor(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "lego_sets.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: LegoDatabase? = null

        fun getInstance(context: Context): LegoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = LegoDatabase(context.applicationContext)
                instance.copyDatabaseFromAssets()
                INSTANCE = instance
                instance
            }
        }
    }

    private fun copyDatabaseFromAssets() {
        val dbPath = context.getDatabasePath(DATABASE_NAME)

        // Check the source file size in assets
        try {
            val assetFileDescriptor = context.assets.openFd(DATABASE_NAME)
            val assetFileSize = assetFileDescriptor.length
            assetFileDescriptor.close()
            android.util.Log.d("LegoDatabase", "Source database in assets size: $assetFileSize bytes")
        } catch (e: Exception) {
            android.util.Log.e("LegoDatabase", "Error checking asset file size: ${e.message}")
        }

        // Delete existing database if it's too small (corrupted/empty)
        if (dbPath.exists() && dbPath.length() < 100000) {
            android.util.Log.d("LegoDatabase", "Deleting corrupted/empty database (${dbPath.length()} bytes)")
            dbPath.delete()
        }

        // Only copy if database doesn't exist
        if (!dbPath.exists()) {
            try {
                // Ensure parent directory exists
                dbPath.parentFile?.mkdirs()

                android.util.Log.d("LegoDatabase", "Copying database from assets...")

                // Copy from assets
                context.assets.open(DATABASE_NAME).use { input ->
                    FileOutputStream(dbPath).use { output ->
                        val buffer = ByteArray(8192)
                        var length: Int
                        var totalBytes = 0L
                        while (input.read(buffer).also { length = it } > 0) {
                            output.write(buffer, 0, length)
                            totalBytes += length
                        }
                        output.flush()
                        android.util.Log.d("LegoDatabase", "Total bytes copied: $totalBytes")
                    }
                }

                android.util.Log.d("LegoDatabase", "Database copied successfully to: ${dbPath.absolutePath}")
                android.util.Log.d("LegoDatabase", "Final database size: ${dbPath.length()} bytes")
            } catch (e: Exception) {
                android.util.Log.e("LegoDatabase", "Error copying database: ${e.message}", e)
                throw RuntimeException("Failed to copy database from assets", e)
            }
        } else {
            android.util.Log.d("LegoDatabase", "Database already exists at: ${dbPath.absolutePath}")
            android.util.Log.d("LegoDatabase", "Database size: ${dbPath.length()} bytes")
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Database is copied from assets, so onCreate is not needed
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades here if needed
    }

    // Get all sets
    fun getAllSets(): List<LegoSet> {
        val sets = mutableListOf<LegoSet>()
        val db = readableDatabase

        val cursor = db.rawQuery("SELECT * FROM sets ORDER BY name", null)

        cursor.use {
            while (it.moveToNext()) {
                sets.add(cursorToLegoSet(it))
            }
        }

        return sets
    }

    // Get sets by theme
    fun getSetsByTheme(theme: String): List<LegoSet> {
        val sets = mutableListOf<LegoSet>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM sets WHERE theme = ? ORDER BY name",
            arrayOf(theme)
        )

        cursor.use {
            while (it.moveToNext()) {
                sets.add(cursorToLegoSet(it))
            }
        }

        return sets
    }

    // Get set by set number
    fun getSetByNumber(setNumber: String): LegoSet? {
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM sets WHERE set_number = ?",
            arrayOf(setNumber)
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToLegoSet(it)
            }
        }

        return null
    }

    // Get set number by UPC
    fun getSetNumberByUPC(upc: String): String? {
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT set_number FROM sets WHERE upc = ?",
            arrayOf(upc)
        )

        cursor.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }

        return null
    }

    // Get full set by UPC
    fun getSetByUPC(upc: String): LegoSet? {
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM sets WHERE upc = ?",
            arrayOf(upc)
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToLegoSet(it)
            }
        }

        return null
    }

    // Search sets by name
    fun searchSetsByName(query: String): List<LegoSet> {
        val sets = mutableListOf<LegoSet>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM sets WHERE name LIKE ? ORDER BY name",
            arrayOf("%$query%")
        )

        cursor.use {
            while (it.moveToNext()) {
                sets.add(cursorToLegoSet(it))
            }
        }

        return sets
    }

    // Get all unique themes
    fun getAllThemes(): List<String> {
        val themes = mutableListOf<String>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT DISTINCT theme FROM sets ORDER BY theme",
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                themes.add(it.getString(0))
            }
        }

        return themes
    }

    // Get sets by year
    fun getSetsByYear(year: Int): List<LegoSet> {
        val sets = mutableListOf<LegoSet>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM sets WHERE year = ? ORDER BY name",
            arrayOf(year.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                sets.add(cursorToLegoSet(it))
            }
        }

        return sets
    }

    // Get sets by piece count range
    fun getSetsByPieceRange(minPieces: Int, maxPieces: Int): List<LegoSet> {
        val sets = mutableListOf<LegoSet>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM sets WHERE pieces BETWEEN ? AND ? ORDER BY pieces",
            arrayOf(minPieces.toString(), maxPieces.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                sets.add(cursorToLegoSet(it))
            }
        }

        return sets
    }

    // Update prices for a set
    fun updatePrices(setNumber: String, newPrice: Double?, usedPrice: Double?): Boolean {
        val db = writableDatabase

        try {
            db.execSQL(
                "UPDATE sets SET new_price = ?, used_price = ? WHERE set_number = ?",
                arrayOf(newPrice, usedPrice, setNumber)
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Get total number of sets
    fun getTotalSetCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM sets", null)

        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }

        return 0
    }

    // Get set count by theme
    fun getSetCountByTheme(theme: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM sets WHERE theme = ?",
            arrayOf(theme)
        )

        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }

        return 0
    }

    // Helper function to convert cursor to LegoSet
    private fun cursorToLegoSet(cursor: android.database.Cursor): LegoSet {
        return LegoSet(
            setId = cursor.getInt(cursor.getColumnIndexOrThrow("set_id")),
            setNumber = cursor.getString(cursor.getColumnIndexOrThrow("set_number")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            theme = cursor.getString(cursor.getColumnIndexOrThrow("theme")),
            subtheme = cursor.getString(cursor.getColumnIndexOrThrow("subtheme")),
            year = cursor.getInt(cursor.getColumnIndexOrThrow("year")),
            pieces = cursor.getInt(cursor.getColumnIndexOrThrow("pieces")),
            usedPrice = if (cursor.isNull(cursor.getColumnIndexOrThrow("used_price"))) null
            else cursor.getDouble(cursor.getColumnIndexOrThrow("used_price")),
            newPrice = if (cursor.isNull(cursor.getColumnIndexOrThrow("new_price"))) null
            else cursor.getDouble(cursor.getColumnIndexOrThrow("new_price")),
            upc = cursor.getString(cursor.getColumnIndexOrThrow("upc")),
            itemNumberNA = cursor.getString(cursor.getColumnIndexOrThrow("item_number_na")),
            imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("image_url")),
            thumbnailUrl = cursor.getString(cursor.getColumnIndexOrThrow("thumbnail_url"))
        )
    }
}

