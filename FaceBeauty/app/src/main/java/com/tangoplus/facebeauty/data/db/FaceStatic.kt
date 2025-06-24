package com.tangoplus.facebeauty.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "t_face_static")
data class FaceStatic (
    @PrimaryKey(autoGenerate = true) val local_sn : Int = 0,
    val server_sn : Int = 0,
    var temp_server_sn: Int = 0,
    var user_uuid: String = "",
    var user_name: String = "",
    var user_mobile: String = "",
    var seq: Int  = 0,
    var mediaFileName: String = "", // 확장자 까지
    var jsonFileName: String = "", // 확장자 까지
    val reg_date: String? = getCurrentDateTime(),
    val resting_eye_horizontal_angle : Float = 0f,
    val resting_earflaps_horizontal_angle : Float = 0f,
    val resting_tip_of_lips_horizontal_angle : Float = 0f,
//    val resting_forehead_glabella_vertical_angle: Float = 0f,
    val resting_glabella_nose_vertical_angle: Float = 0f,
    val resting_nose_chin_vertical_angle : Float = 0f,
    val resting_left_earflaps_nasal_wing_horizontal_angle : Float = 0f,
    val resting_right_earflaps_nasal_wing_horizontal_angle: Float = 0f,
    val resting_left_earflaps_nose_distance : Float = 0f,
    val resting_right_earflaps_nose_distance : Float = 0f,
    val resting_left_tip_of_lips_center_lips_distance : Float = 0f,
    val resting_right_tip_of_lips_center_lips_distance : Float = 0f,

    val occlusal_eye_horizontal_angle : Float = 0f,
    val occlusal_earflaps_horizontal_angle : Float = 0f,
    val occlusal_tip_of_lips_horizontal_angle : Float = 0f,
//    val occlusal_forehead_glabella_vertical_angle: Float = 0f,
    val occlusal_glabella_nose_vertical_angle: Float = 0f,
    val occlusal_nose_chin_vertical_angle : Float = 0f,
    val occlusal_left_earflaps_nasal_wing_horizontal_angle : Float = 0f,
    val occlusal_right_earflaps_nasal_wing_horizontal_angle: Float = 0f,
    val occlusal_left_earflaps_nose_distance : Float = 0f,
    val occlusal_right_earflaps_nose_distance : Float = 0f,
    val occlusal_left_tip_of_lips_center_lips_distance : Float = 0f,
    val occlusal_right_tip_of_lips_center_lips_distance : Float = 0f,

    ) {
    companion object {
        fun getCurrentDateTime(): String =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
}