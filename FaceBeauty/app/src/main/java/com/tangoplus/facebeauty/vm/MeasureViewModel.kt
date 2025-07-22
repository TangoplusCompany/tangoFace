package com.tangoplus.facebeauty.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MeasureViewModel : ViewModel() {
    // ---------------------------- # 측정 과정 #-------------------------
    // 측정값들 담기
    val staticJA = JSONArray()

    // 결과값 임시로 담을 곳 (영상 녹화가 끝나면 초기화)
    var tempStaticJo : JSONObject? = null
    // 중간 임시 저장 좌표들 ( seq 1개씩만 )
    var tempCoordinateJA = JSONArray()

    // 측정 완료 시 저장할 좌표임 seq 6개 전부 들어가는 곳
    var coordinatesJA = JSONArray()
    // 현재 얼굴 좌표들 전부 담는 곳 (형식은 coordinatesJA와 같음)
    var currentCoordinate = mutableListOf<Pair<Float, Float>>() // 절대 좌표
    var relativeCoordinate = mutableListOf<Pair<Float, Float>>() // 상대좌표

    // 각도+좌표가 합쳐진 result
    // ---------------------------- # 저장 전 #-------------------------
    val mergedJA = JSONArray()

    // static 데이터 + 좌표값 통합된 JSON
    val staticFileNames = mutableListOf<String>()

    var initMeasure = MutableLiveData(true)
    var currentUUID = ""

    // -------# pose landmark #-------
    val plrJA = JSONArray()
    var tempPlrJA = JSONArray()
    val currentPlrCoordinate = mutableListOf<Pair<Float, Float>>()

    fun getCurrentDateTime(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}