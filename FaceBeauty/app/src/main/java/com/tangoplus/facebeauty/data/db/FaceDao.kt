package com.tangoplus.facebeauty.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FaceDao {

    @Insert
    fun insertStatic(faceStatic: FaceStatic) : Long

    @Query("SELECT * FROM t_face_static")
    fun getAllData() : List<FaceStatic>

//    @Query("SELECT * FROM t_face_static WHERE user_uuid == :userUUID")
//    fun getU
}