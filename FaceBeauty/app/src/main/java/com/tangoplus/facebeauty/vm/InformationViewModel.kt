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


    private val ratioLines = listOf(DrawRatioLine.A_ALL, DrawRatioLine.A_VERTI, DrawRatioLine.A_HORIZON, DrawRatioLine.A_NONE)
    private var currentRatioLine = DrawRatioLine.A_NONE
    fun setRatioState() {
        val currentIndex = ratioLines.indexOf(currentRatioLine)
        val nextIndex = (currentIndex + 1) % ratioLines.size
        currentRatioLine = ratioLines[nextIndex]
    }
    fun setAllOrNone() {
        when (currentRatioLine) {
            DrawRatioLine.A_NONE -> {
                currentRatioLine = DrawRatioLine.A_ALL
            }
            else -> {
                currentRatioLine = DrawRatioLine.A_NONE
            }
        }

    }
    fun getRatioState() : DrawRatioLine {
        return currentRatioLine
    }

}