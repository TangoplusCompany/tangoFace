package com.tangoplus.facebeauty.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.facebeauty.data.DrawLine
import com.tangoplus.facebeauty.data.DrawRatioLine
import com.tangoplus.facebeauty.data.FaceComparisonItem
import com.tangoplus.facebeauty.data.FaceLandmarkResult
import com.tangoplus.facebeauty.data.FaceResult

class GalleryViewModel : ViewModel() {
    val currentFaceResults = mutableListOf<FaceResult>()
    var isShowResult = MutableLiveData(false)
    val currentResult = MutableLiveData<FaceResult>()
    val currentCheckedLines = mutableSetOf<DrawLine>()
    val currentCheckedRatioLines = mutableSetOf<DrawRatioLine>()

    var currentFaceComparision = mutableListOf<FaceComparisonItem>()
}