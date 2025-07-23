package com.tangoplus.facebeauty.vision.face

import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.tangoplus.facebeauty.data.FaceLandmarkResult

object FaceLandmarkAdapter {
    fun toCustomFaceLandmarkResult(FaceLandmarkerResult: FaceLandmarkerResult): FaceLandmarkResult {
        val landmarks = FaceLandmarkerResult.faceLandmarks().firstOrNull()?.map { landmark ->
            FaceLandmarkResult.FaceLandmark(
                x = landmark.x(),
                y = landmark.y(),

                )
        } ?: emptyList()

        return FaceLandmarkResult(landmarks)
    }
}