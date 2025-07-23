package com.tangoplus.facebeauty.vm

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangoplus.facebeauty.data.FaceDisplay
import com.tangoplus.facebeauty.data.FaceResult
import com.tangoplus.facebeauty.data.db.FaceDatabase
import com.tangoplus.facebeauty.data.db.FaceStatic
import com.tangoplus.facebeauty.util.FileUtility.getImageUriFromFileName
import com.tangoplus.facebeauty.util.FileUtility.getJsonUriFromFileName
import com.tangoplus.facebeauty.util.FileUtility.getVideoUriFromFileName
import com.tangoplus.facebeauty.util.FileUtility.readJsonFromUri
import com.tangoplus.facebeauty.vision.face.FaceLandmarkerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel : ViewModel() {

    private var _delegate: Int = FaceLandmarkerHelper.DELEGATE_CPU
    private var _minFaceDetectionConfidence: Float =
        FaceLandmarkerHelper.DEFAULT_FACE_DETECTION_CONFIDENCE
    private var _minFaceTrackingConfidence: Float = FaceLandmarkerHelper
        .DEFAULT_FACE_TRACKING_CONFIDENCE
    private var _minFacePresenceConfidence: Float = FaceLandmarkerHelper
        .DEFAULT_FACE_PRESENCE_CONFIDENCE
    private var _maxFaces: Int = FaceLandmarkerHelper.DEFAULT_NUM_FACES

    val currentDelegate: Int get() = _delegate
    val currentMinFaceDetectionConfidence: Float
        get() =
            _minFaceDetectionConfidence
    val currentMinFaceTrackingConfidence: Float
        get() =
            _minFaceTrackingConfidence
    val currentMinFacePresenceConfidence: Float
        get() =
            _minFacePresenceConfidence
    val currentMaxFaces: Int get() = _maxFaces

    private var isStartCountDown = false

    private var isGuideTextAnimationFinished = false

    private var isGuideTextChanged = false

    private var isSeqFinished = false


    fun setDelegate(delegate: Int) {
        _delegate = delegate
    }

    fun setMinFaceDetectionConfidence(confidence: Float) {
        _minFaceDetectionConfidence = confidence
    }
    fun setMinFaceTrackingConfidence(confidence: Float) {
        _minFaceTrackingConfidence = confidence
    }
    fun setMinFacePresenceConfidence(confidence: Float) {
        _minFacePresenceConfidence = confidence
    }

    fun setMaxFaces(maxResults: Int) {
        _maxFaces = maxResults
    }

    fun setCountDownFlag(isStart: Boolean) {
        isStartCountDown = isStart
    }
    fun getCountDownFlag() : Boolean {
        return isStartCountDown
    }

    fun setTextAnimationFlag(isStart: Boolean) {
        isGuideTextAnimationFinished = isStart
    }
    fun getTextAnimationFlag() : Boolean {
        return isGuideTextAnimationFinished
    }

    fun setGuideTextFlag(isStart: Boolean) {
        isGuideTextChanged = isStart
    }
    fun getGuideTextFlag() : Boolean {
        return isGuideTextChanged
    }
    fun setSeqFinishedFlag(isStart: Boolean) {
        isSeqFinished = isStart
    }
    fun getSeqFinishedFlag() : Boolean {
        return isSeqFinished
    }

    // 초기 db에서 데이터 전부 담기 + adapter에서 쓸 DTO 만들기
//    val currentFaceResults = mutableListOf<FaceResult>()

    val currentFaceDisplays = mutableListOf<FaceDisplay>()
    val dataLoadComplete = MutableLiveData<Boolean>()
    fun loadDataFromDB(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = FaceDatabase.getDatabase(context).faceDao()
            val allData = dao.getAllDisplayData()

            currentFaceDisplays.clear()
            currentFaceDisplays.addAll(allData)
            currentFaceDisplays.sortByDescending { it.tempServerSn }
            Log.v("currentDisplays", "${currentFaceDisplays.map { it.tempServerSn }}")
//            val allData = dao.getAllData()
//
//            currentFaceResults.clear()
//            Log.v("전부", "${allData.map { it.temp_server_sn }}")
//            val aa = convertToFaceResults(context, allData)
//            aa.forEach {
//                currentFaceResults.add(it)
//            }
//            currentFaceResults.sortByDescending { it.tempServerSn }
//
//            val displayItems = currentFaceResults.map {
//                FaceDisplay(
//                    tempServerSn = it.tempServerSn,
//                    userName = it.userName,
//                    userMobile = it.userMobile,
//                    regDate = it.regDate
//                )
//            }
//            displayList.postValue(displayItems)

            dataLoadComplete.postValue(true)

        }
    }



    fun convertToFaceResult(context: Context, faceStatics: List<FaceStatic>): FaceResult {
        val mediaUri = mutableListOf<Uri?>()
        val jsonArray = JSONArray()

        faceStatics.forEachIndexed { indexx ,faceStatic ->
            // 1. mediaFileUri를 Uri로 변환해서 추가
            try {
                Log.v("영상URI", "$indexx ${faceStatic.media_file_name}")
                if (indexx in listOf(2, 3)) {
                    val videoUri = getVideoUriFromFileName(context, faceStatic.media_file_name)
                    mediaUri.add(videoUri)
                } else {
                    val imageUri = getImageUriFromFileName(context, faceStatic.media_file_name)
                    mediaUri.add(imageUri)
                }
            } catch (e: Exception) {
                mediaUri.add(null)
                Log.e("FaceResultConverter", "이미지 URI 파싱 실패: ${faceStatic.media_file_name}", e)
            }

            // 2. jsonFileUri에서 JSON 파일 읽어서 JSONObject로 변환
            try {
                val jsonUri = getJsonUriFromFileName(context, faceStatic.json_file_name)
                val jsonObject = jsonUri?.let { readJsonFromUri(context, it) }
                Log.v("jsonObject있나요?", "$jsonObject")
                jsonArray.put(jsonObject)
            } catch (e: Exception) {
                Log.e("FaceResultConverter", "JSON 파일 읽기 실패: ${faceStatic.json_file_name}", e)
                // 빈 JSONObject라도 추가해서 배열 순서 맞추기
                jsonArray.put(JSONObject())
            }
        }
        return FaceResult(
            tempServerSn = faceStatics[0].temp_server_sn,
            userName = faceStatics[0].user_name,
            userMobile = faceStatics[0].user_mobile,
            mediaUri = mediaUri,
            results = jsonArray,
            regDate = faceStatics[0].reg_date
        )
    }

//    private fun convertToFaceResults(context: Context,  faceStaticList: List<FaceStatic>): List<FaceResult> {
//
//        val existingTempServerSns = currentFaceResults.map { it.tempServerSn }.toSet()
//
//        // temp_server_sn으로 그룹화하되, 이미 존재하는 것은 제외
//        val groupedData = faceStaticList
//            .filter { it.temp_server_sn >= 0 && !existingTempServerSns.contains(it.temp_server_sn) }
//            .groupBy { it.temp_server_sn }
//
//        Log.v("그룹된데이터", "$groupedData")
//        Log.v("기존데이터", "기존 tempServerSns: $existingTempServerSns")
//
//        return groupedData.map { (_, faceStatics) ->
//            convertToFaceResult(context, faceStatics)
//        }
//    }

    // 비교일 경우
    private var _comparisonState = MutableLiveData<Boolean>()
    val comparisonState: LiveData<Boolean> = _comparisonState

    fun setComparisonState(isComparison: Boolean) {
        _comparisonState.value = isComparison
    }

    fun getComparisonState(): Boolean {
        return _comparisonState.value ?: false
    }

    var comparisonDoubleItem : Pair<FaceResult, FaceResult>? = null // null 일경우 1개 선택 한 상황 not null일 경우 비교상황

    private val _tempComparisonItems = MutableLiveData<MutableList<FaceDisplay>>(mutableListOf())
    val tempComparisonItems : LiveData<MutableList<FaceDisplay>> = _tempComparisonItems
    fun addItem(item: FaceDisplay) {
        val currentList = _tempComparisonItems.value ?: mutableListOf()
        currentList.add(item)
        _tempComparisonItems.value = currentList.toMutableList()
    }

    fun removeItem(item: FaceDisplay) {
        val currentList = _tempComparisonItems.value ?: mutableListOf()
        currentList.remove(item)
        _tempComparisonItems.value = currentList.toMutableList()
    }
    fun clearItems() {
        val currentList = _tempComparisonItems.value ?: mutableListOf()
        currentList.clear()
        _tempComparisonItems.value = currentList.toMutableList()
    }
    val currentResult = MutableLiveData<FaceResult>()

    var isMeasureFinish = false
}