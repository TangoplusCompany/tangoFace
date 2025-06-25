package com.tangoplus.facebeauty.vm

import androidx.lifecycle.ViewModel
import com.tangoplus.facebeauty.vision.FaceLandmarkerHelper

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
}