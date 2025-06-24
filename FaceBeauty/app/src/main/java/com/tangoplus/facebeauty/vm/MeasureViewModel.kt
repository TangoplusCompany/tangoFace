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
    var staticJson0 = JSONObject()
    var staticJson1 = JSONObject()

    // faceLandmark 좌표들
    var coordinates0 = JSONArray()
    var coordinates1 = JSONArray()

    var currentFaceLandmarks = JSONArray()
    // 현재 얼굴 좌표들 전부 담는 곳 ( 절대 좌표)
    var currentCoordinate = mutableListOf<Pair<Float, Float>>()
    var relativeCoordinate = mutableListOf<Pair<Float, Float>>()
    // static 데이터 + 좌표값 통합된 JSON
    var mergedJson0 = JSONObject()
    var mergedJson1 = JSONObject()

    var static0FileName : String? = null
    var static1FileName : String? = null


    var initMeasure = MutableLiveData(true)
    var currentUUID = ""



    fun getCurrentDateTime(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}