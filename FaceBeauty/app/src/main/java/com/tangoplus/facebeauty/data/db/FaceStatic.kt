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
    var media_file_name: String = "", // 확장자 까지
    var json_file_name: String = "", // 확장자 까지
    val reg_date: String? = getCurrentDateTime(),

    val resting_eye_horizontal_angle : Float = 0f,
    val resting_eyebrow_horizontal_angle : Float = 0f,
    val resting_tip_of_lips_horizontal_angle : Float = 0f,
    val resting_tip_of_chin_horizontal_angle : Float = 0f,
    val resting_canthus_oral_left_vertical_angle : Float = 0f,
    val resting_canthus_oral_right_vertical_angle : Float = 0f,
    val resting_nasal_wing_tip_of_lips_left_vertical_angle: Float = 0f,
    val resting_nasal_wing_tip_of_lips_right_vertical_angle: Float = 0f,
    val resting_left_cheeks_extent : Float = 0f,
    val resting_right_cheeks_extent : Float = 0f,

    val occlusal_eye_horizontal_angle : Float = 0f,
    val occlusal_eyebrow_horizontal_angle : Float = 0f,
    val occlusal_tip_of_lips_horizontal_angle : Float = 0f,
    val occlusal_tip_of_chin_horizontal_angle : Float = 0f,
    val occlusal_canthus_oral_left_vertical_angle : Float = 0f,
    val occlusal_canthus_oral_right_vertical_angle : Float = 0f,
    val occlusal_nasal_wing_tip_of_lips_left_vertical_angle: Float = 0f,
    val occlusal_nasal_wing_tip_of_lips_right_vertical_angle: Float = 0f,
    val occlusal_left_cheeks_extent : Float = 0f,
    val occlusal_right_cheeks_extent : Float = 0f,


    val jaw_left_tilt_nose_chin_vertical_angle : Float = 0f,
    val jaw_left_tilt_tip_of_lips_horizontal_angle : Float = 0f,
    val jaw_left_tilt_left_mandibular_distance : Float = 0f,
    val jaw_left_tilt_right_mandibular_distance : Float = 0f,

    val jaw_right_tilt_nose_chin_vertical_angle : Float = 0f,
    val jaw_right_tilt_tip_of_lips_horizontal_angle : Float = 0f,
    val jaw_right_tilt_left_mandibular_distance : Float = 0f,
    val jaw_right_tilt_right_mandibular_distance : Float = 0f,

    val jaw_opening_lips_distance : Float = 0f,
    val jaw_opening_lips_vertical_angle : Float = 0f,

    val neck_extention_shoulder_horizontal_angle : Float = 0f,
    val neck_extention_ear_horizontal_angle : Float = 0f,
    val neck_extention_neck_vertical_angle: Float = 0f
    ) {
    companion object {
        fun getCurrentDateTime(): String =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
}