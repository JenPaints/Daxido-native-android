package com.daxido.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.daxido.core.database.dao.RideDao
import com.daxido.core.database.dao.UserDao
import com.daxido.core.database.dao.LocationCacheDao
import com.daxido.core.database.entities.RideEntity
import com.daxido.core.database.entities.UserEntity
import com.daxido.core.database.entities.LocationCacheEntity

/**
 * OFFLINE MODE: Room Database for local data persistence
 * Enables offline functionality when network is unavailable
 *
 * Database Migration Strategy:
 * - Version 1: Initial schema with Rides, Users, LocationCache
 * - Future versions: Add migration strategies in companion object
 */
@Database(
    entities = [
        RideEntity::class,
        UserEntity::class,
        LocationCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DaxidoDatabase : RoomDatabase() {

    abstract fun rideDao(): RideDao
    abstract fun userDao(): UserDao
    abstract fun locationCacheDao(): LocationCacheDao

    companion object {
        @Volatile
        private var INSTANCE: DaxidoDatabase? = null

        private const val DATABASE_NAME = "daxido_database"

        fun getInstance(context: Context): DaxidoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DaxidoDatabase::class.java,
                    DATABASE_NAME
                )
                    // Production-ready migration strategy
                    .addMigrations(
                        // Add future migrations here as needed
                        // MIGRATION_1_2,
                        // MIGRATION_2_3,
                    )
                    // Only use destructive migration in debug builds
                    .apply {
                        if (com.daxido.BuildConfig.DEBUG) {
                            fallbackToDestructiveMigration()
                        }
                    }
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * DATABASE MIGRATION STRATEGY
         *
         * Production Migration Examples:
         */

        // Example: Migration from version 1 to 2
        // Uncomment and modify when you need to add new fields
        /*
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: Add rating column to rides table
                database.execSQL(
                    "ALTER TABLE rides ADD COLUMN rating REAL NOT NULL DEFAULT 0.0"
                )
            }
        }
        */

        // Example: Migration from version 2 to 3
        /*
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: Create new table for favorites
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorites (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        locationName TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        */

        /**
         * Migration Best Practices:
         * 1. Never use fallbackToDestructiveMigration() in production
         * 2. Test migrations thoroughly with real user data
         * 3. Keep all migration objects in version control
         * 4. Document schema changes in comments
         * 5. Consider data transformation during migration
         * 6. Always provide a default value for new NOT NULL columns
         */
    }
}
