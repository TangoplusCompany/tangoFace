package com.tangoplus.facebeauty.vm

import androidx.lifecycle.ViewModel
import com.tangoplus.facebeauty.data.DrawLine
import com.tangoplus.facebeauty.data.DrawRatioLine
import com.tangoplus.facebeauty.data.FaceComparisonItem

class InformationViewModel : ViewModel() {
    val currentCheckedLines = mutableSetOf<DrawLine>()
    val currentCheckedRatioLines = mutableSetOf<DrawRatioLine>()

    // 체크된 항목 담는 곳
    var currentFaceComparision = mutableListOf<FaceComparisonItem>()


    private var currentSeqIndex = 0
    fun setSeqIndex(indexx: Int) {
        currentSeqIndex = indexx
    }
    fun getSeqIndex() : Int {
        return currentSeqIndex
    }
}