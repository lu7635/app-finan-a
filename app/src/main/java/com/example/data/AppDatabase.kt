package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Client::class, Debt::class, Payment::class, Product::class, UserProfile::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE debts ADD COLUMN dueDate INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE debts ADD COLUMN receivedAt INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE payments ADD COLUMN status TEXT NOT NULL DEFAULT 'Recebido'")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE payments ADD COLUMN receivedAt INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("UPDATE payments SET receivedAt = createdAt WHERE receivedAt = 0")
                } catch (e: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "controle_fiado_database"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
