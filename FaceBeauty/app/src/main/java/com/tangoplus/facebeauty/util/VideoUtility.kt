package com.tangoplus.facebeauty.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object VideoUtility {
    fun getVideoDimensions(context : Context, videoUri: Uri?) : Pair<Int, Int> {
        if (videoUri == null) {
//            Log.e("videoUri", "$videoUri")
            return Pair(0, 0) // 기본값 반환
        }
//        Log.e("videoUri", "$videoUri")
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        retriever.release()
        return Pair(videoWidth, videoHeight)
    }

    fun extractVideoCoordinates(jsonData: JSONObject): List<List<Pair<Float, Float>>> {
        val faceLandmarks = jsonData.getJSONArray("face_landmark")

        return List(faceLandmarks.length()) { i ->
            val frameLandmarks = faceLandmarks.getJSONArray(i)

            List(frameLandmarks.length()) { j ->
                val landmark = frameLandmarks.getJSONArray(j)
                Pair(
                    landmark.getDouble(1).toFloat(),  // x
                    landmark.getDouble(2).toFloat()   // y
                )
            }
        }
    }
}