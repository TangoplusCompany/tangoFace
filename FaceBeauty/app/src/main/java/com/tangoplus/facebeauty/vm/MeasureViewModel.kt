package com.tangoplus.facebeauty.vm

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.facebeauty.data.FaceResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MeasureViewModel : ViewModel() {
    // static데이터만 담는 곳
    val staticJA = JSONArray()
    // 중간 임시 저장 좌표들
    var tempCoordinateJA = JSONArray()
    // 측정 완료 시 저장할 좌표 저장
    var coordinatesJA = JSONArray()

    // 각도+좌표가 합쳐진 result
    val mergedJA = JSONArray()

    var currentFaceLandmarks = JSONArray()
    // 현재 얼굴 좌표들 전부 담는 곳 ( 절대 좌표)
    var currentCoordinate = mutableListOf<Pair<Float, Float>>()
    var relativeCoordinate = mutableListOf<Pair<Float, Float>>()
    // static 데이터 + 좌표값 통합된 JSON
    val staticFileNames = mutableListOf<String>()

    var initMeasure = MutableLiveData(true)
    var currentUUID = ""

    val plrJA = JSONArray()
    var tempPlrJA = JSONArray()
    val currentPlrCoordinate = mutableListOf<Pair<Float, Float>>()

    fun getCurrentDateTime(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}