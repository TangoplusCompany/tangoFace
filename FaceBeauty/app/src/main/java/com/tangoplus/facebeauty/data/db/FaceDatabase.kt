package com.tangoplus.facebeauty.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FaceStatic::class], version = 1)
abstract class FaceDatabase : RoomDatabase() {
    abstract fun faceDao() : FaceDao

    companion object {
        @Volatile
        private var INSTANCE: FaceDatabase? = null
        fun getDatabase(context: Context): FaceDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FaceDatabase::class.java,
                    "face_database"
                )
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