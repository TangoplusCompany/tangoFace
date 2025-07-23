package com.tangoplus.facebeauty.ui.view

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.tangoplus.facebeauty.data.FaceLandmarkResult
import com.tangoplus.facebeauty.vm.MeasureViewModel
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: FaceLandmarkResult? = null
    private var outLinePaint = Paint()
    private var standardPaint = Paint()
    private var notStandardPaint = Paint()
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private var verticalLineSuccess = false
    private var horizontalLineSuccess = false

    fun setVerti(isSuccess: Boolean) {
        verticalLineSuccess = isSuccess
    }

    fun setHorizon(isSuccess: Boolean) {
        horizontalLineSuccess = isSuccess
    }
    init {
        initPaints()
    }
    // TODO seq2 일 때 verti, horizon에 대해서 다른 각도로 접근 해야함
    // TODO 잘 촬영이 되게끔
    // TODO 사진 어떻게 보여줄건지 4장 2장씩 seq 2 페이징
    fun getVerti() : Boolean {
        return verticalLineSuccess
    }
    fun getHorizon() : Boolean {
        return horizontalLineSuccess
    }

    fun clear() {
        results = null
        standardPaint.reset()
        notStandardPaint.reset()
        outLinePaint.reset()
        invalidate()
        initPaints()
    }
    enum class RunningMode {
        IMAGE, VIDEO, LIVE_STREAM
    }
    private fun initPaints() {

        standardPaint = Paint().apply {
            color = "#8000FF00".toColorInt()
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
            maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.SOLID)
        }
        notStandardPaint = Paint().apply {
            color = "#80FF2819".toColorInt()
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
            maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.SOLID)
        }
        outLinePaint = Paint().apply {
            color = "#FFFFFF".toColorInt()
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
//            maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // Clear previous drawings if results exist but have no face landmarks
        if (results?.landmarks.isNullOrEmpty()) {
            clear()
            return
        }

        results?.let { faceLandmarkerResult ->

            val scaledImageWidth = imageWidth * scaleFactor
            val scaledImageHeight = imageHeight * scaleFactor

            val offsetX = (width - scaledImageWidth) / 2f
            val offsetY = (height - scaledImageHeight) / 2f
            drawFaceLandmarks(canvas, faceLandmarkerResult.landmarks, offsetX, offsetY)
//            faceLandmarkerResult.landmarks.forEach { faceLandmarks ->
//
//            }
        }
    }


    private fun drawFaceLandmarks(
        canvas: Canvas,
        faceLandmarks: List<FaceLandmarkResult.FaceLandmark>,
        offsetX: Float,
        offsetY: Float
    ) {

        // x, y에 들어가있는 좌표는?
//        val faceOutLineIndexes = listOf(
//            10, 109, 67, 103, 54, 21, 162, 127, 234, 93, 132, 58, 172, 136, 150, 149, 176, 148,
//            152, 377, 378, 379, 365, 397, 288, 361, 323, 454, 356, 389, 251, 284, 332, 297, 338, 10
//        )
//        val outLinePath = Path()
////        Log.v("얼굴테두리각형갯수", "${faceOutLineIndexes.size}")
//        faceOutLineIndexes.forEachIndexed { i, landmarkIndex ->
//            val landmark = faceLandmarks[landmarkIndex]
//            val x = landmark.x() * imageWidth * scaleFactor + offsetX
//            val y = landmark.y() * imageHeight * scaleFactor + offsetY
//
//            if (i == 0) {
//                outLinePath.moveTo(x, y) // 시작점
//            } else {
//                outLinePath.lineTo(x, y) // 선 연결
//            }
//        }
//        canvas.drawPath(outLinePath, outLinePaint)

        // --------------------------# 10 - 1 - 152 양귓볼 - 코 #------------------------------
        val horizontalEarFlapIndexes = listOf(234, 1, 454)
        val horizontalEarFlapLinePath = Path()
        val horizontalPoints = horizontalEarFlapIndexes.map { landmarkIndex ->
            val landmark = faceLandmarks[landmarkIndex]
            val x = landmark.x * imageWidth * scaleFactor + offsetX
            val y = landmark.y * imageHeight * scaleFactor + offsetY
            PointF(x, y)
        }
        if (horizontalPoints.isNotEmpty()) {
            horizontalEarFlapLinePath.moveTo(horizontalPoints[0].x, horizontalPoints[0].y)

            for (i in 1 until horizontalPoints.size) {
                val prev = horizontalPoints[i - 1]
                val curr = horizontalPoints[i]

                // 이전 점과 현재 점의 중간 지점을 목표로 부드럽게 이어줌
                val midX = (prev.x + curr.x) / 2
                val midY = (prev.y + curr.y) / 2

                horizontalEarFlapLinePath.quadTo(prev.x, prev.y, midX, midY)
            }
            horizontalEarFlapLinePath.lineTo(horizontalPoints.last().x, horizontalPoints.last().y)
        }
        val horizonLinePaint = if (horizontalLineSuccess) standardPaint else notStandardPaint
        canvas.drawPath(horizontalEarFlapLinePath, horizonLinePaint)


        // --------------------------# 10 - 1 - 152 이마 - 턱 #------------------------------
        val verticalIndexes = listOf(10, 1, 152)
        val verticalPath = Path()
        val verticalPoints = verticalIndexes.map { landmarkIndex ->
            val landmark = faceLandmarks[landmarkIndex]
            val x = landmark.x * imageWidth * scaleFactor + offsetX
            val y = landmark.y * imageHeight * scaleFactor + offsetY
            PointF(x, y)
        }
        if (verticalPoints.isNotEmpty()) {
            verticalPath.moveTo(verticalPoints[0].x, verticalPoints[0].y)

            for (i in 1 until verticalPoints.size) {
                val prev = verticalPoints[i - 1]
                val curr = verticalPoints[i]

                // 이전 점과 현재 점의 중간 지점을 목표로 부드럽게 이어줌
                val midX = (prev.x + curr.x) / 2
                val midY = (prev.y + curr.y) / 2
                verticalPath.quadTo(prev.x, prev.y, midX, midY)
            }
            verticalPath.lineTo(verticalPoints.last().x, verticalPoints.last().y)
        }
        val vertiLinePaint = if (verticalLineSuccess) standardPaint else notStandardPaint
        canvas.drawPath(verticalPath, vertiLinePaint)

    }

    fun setResults(
        faceLandmarkerResults: FaceLandmarkResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = faceLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO  -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 2F
        private const val TAG = "Face Landmarker Overlay"
    }
}
