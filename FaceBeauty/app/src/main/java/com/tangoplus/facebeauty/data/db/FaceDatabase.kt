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
                // 1. 새 테이블 생성 (임시 이름 사용)
                db.execSQL("""
            CREATE TABLE t_face_static_new (
                local_sn INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                server_sn INTEGER NOT NULL,
                temp_server_sn INTEGER NOT NULL,
                user_uuid TEXT NOT NULL,
                user_name TEXT NOT NULL,
                user_mobile TEXT NOT NULL,
                seq INTEGER NOT NULL,
                mediaFileName TEXT NOT NULL,
                jsonFileName TEXT NOT NULL,
                reg_date TEXT,

                resting_eye_horizontal_angle REAL NOT NULL,
                resting_eyebrow_horizontal_angle REAL NOT NULL,
                resting_tip_of_lips_horizontal_angle REAL NOT NULL,
                resting_tip_of_chin_horizontal_angle REAL NOT NULL,
                resting_canthus_oral_left_vertical_angle REAL NOT NULL,
                resting_canthus_oral_right_vertical_angle REAL NOT NULL,
                resting_nasal_wing_tip_of_lips_left_vertical_angle REAL NOT NULL,
                resting_nasal_wing_tip_of_lips_right_vertical_angle REAL NOT NULL,
                resting_left_cheeks_extent REAL NOT NULL,
                resting_right_cheeks_extent REAL NOT NULL,

                occlusal_eye_horizontal_angle REAL NOT NULL,
                occlusal_eyebrow_horizontal_angle REAL NOT NULL,
                occlusal_tip_of_lips_horizontal_angle REAL NOT NULL,
                occlusal_tip_of_chin_horizontal_angle REAL NOT NULL,
                occlusal_canthus_oral_left_vertical_angle REAL NOT NULL,
                occlusal_canthus_oral_right_vertical_angle REAL NOT NULL,
                occlusal_nasal_wing_tip_of_lips_left_vertical_angle REAL NOT NULL,
                occlusal_nasal_wing_tip_of_lips_right_vertical_angle REAL NOT NULL,
                occlusal_left_cheeks_extent REAL NOT NULL,
                occlusal_right_cheeks_extent REAL NOT NULL,

                jaw_left_tilt_nose_chin_vertical_angle REAL NOT NULL,
                jaw_left_tilt_tip_of_lips_horizontal_anngle REAL NOT NULL,
                jaw_left_tilt_left_mandibular_distance REAL NOT NULL,
                jaw_left_tilt_right_mandibular_distance REAL NOT NULL,

                jaw_right_tilt_nose_chin_vertical_angle REAL NOT NULL,
                jaw_right_tilt_tip_of_lips_horizontal_anngle REAL NOT NULL,
                jaw_right_tilt_left_mandibular_distance REAL NOT NULL,
                jaw_right_tilt_right_mandibular_distance REAL NOT NULL,

                jaw_opening_lips_distance REAL NOT NULL,
                jaw_opening_lips_vertical_angle REAL NOT NULL,

                neck_extention_shoulder_horizontal_angle REAL NOT NULL,
                neck_extention_ear_horizontal_angle REAL NOT NULL,
                neck_extention_neck_vertical_angle REAL NOT NULL
            )
        """)
                // 3. 기존 테이블 삭제
                db.execSQL("DROP TABLE t_face_static")

                // 4. 새 테이블 이름 변경
                db.execSQL("ALTER TABLE t_face_static_new RENAME TO t_face_static")
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
                ).addMigrations(MIGRATION_1_2)
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