package com.tangoplus.facebeauty.data

import android.net.Uri
import org.json.JSONArray

data class FaceResult(
    val tempServerSn: Int,
    val userName: String? = null,
    val userMobile: String? = null,
    val imageUris : List<Uri?> = listOf(),
    val results: JSONArray = JSONArray(),
    var regDate : String? = null
)
