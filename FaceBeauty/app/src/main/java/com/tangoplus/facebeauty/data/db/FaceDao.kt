package com.tangoplus.facebeauty.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tangoplus.facebeauty.data.FaceDisplay

@Dao
interface FaceDao {

    @Insert
    fun insertStatic(faceStatic: FaceStatic) : Long

    @Query("SELECT * FROM t_face_static")
    fun getAllData() : List<FaceStatic>

    @Query("""
    SELECT 
        temp_server_sn AS tempServerSn,
        MIN(user_name) AS userName,
        MIN(user_mobile) AS userMobile,
        MIN(reg_date) AS regDate
    FROM t_face_static
    GROUP BY temp_server_sn
    """)
    fun getAllDisplayData(): List<FaceDisplay>

    @Query("SELECT * FROM t_face_static WHERE temp_server_sn == :tempServerSn ORDER BY seq ASC")
    fun getStatic(tempServerSn : Int) : List<FaceStatic>

//    @Query("SELECT * FROM t_face_static WHERE user_uuid == :userUUID")
//    fun getU
}