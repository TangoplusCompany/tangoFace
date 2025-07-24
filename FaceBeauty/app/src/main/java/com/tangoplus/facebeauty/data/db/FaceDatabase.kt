package com.tangoplus.facebeauty.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [FaceStatic::class], version = 2)
abstract class FaceDatabase : RoomDatabase() {
    abstract fun faceDao() : FaceDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE your_table_name ADD COLUMN jaw_opening_lips_left_cheeks_extent REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE your_table_name ADD COLUMN jaw_opening_lips_right_cheeks_extent REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE your_table_name ADD COLUMN neck_extention_left_mandibular_chin_angle REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE your_table_name ADD COLUMN neck_extention_right_mandibular_chin_angle REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE your_table_name ADD COLUMN neck_extention_middle_mandibular_chin_angle REAL NOT NULL DEFAULT 0")
            }
        }


        @Volatile
        private var INSTANCE: FaceDatabase? = null
        fun getDatabase(context: Context): FaceDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FaceDatabase::class.java,
                    "face_database"
                ).fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}