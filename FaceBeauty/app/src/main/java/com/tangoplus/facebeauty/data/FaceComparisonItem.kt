package com.tangoplus.facebeauty.data

data class FaceComparisonItem(
    val label: String,
    val leftValue: Float? = null,
    val rightValue: Float? = null,
    var isChecked: Boolean = false,
    var state : Int = 0,
    val type: RVItemType = RVItemType.NORMAL
)