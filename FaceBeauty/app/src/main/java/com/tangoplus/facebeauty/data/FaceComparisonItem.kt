package com.tangoplus.facebeauty.data

data class FaceComparisonItem(
    val label: String,
    val restingValue: Float,
    val occlusalValue: Float,
    var isChecked: Boolean = false,
    val type: RVItemType = RVItemType.NORMAL
)