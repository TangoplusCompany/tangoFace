package com.tangoplus.facebeauty.ui.view

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.graphics.toColorInt
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: FaceLandmarkerResult? = null
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

    private fun initPaints() {

        standardPaint = Paint().apply {
            color = "#8000C9DC".toColorInt()
            strokeWidth = 4f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        notStandardPaint = Paint().apply {
            color = "#80FF2819".toColorInt()
            strokeWidth = 4f
            isAntiAlias = true
            style = Paint.Style.STROKE
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
        if (results?.faceLandmarks().isNullOrEmpty()) {
            clear()
            return
        }

        results?.let { faceLandmarkerResult ->

            // Calculate scaled image dimensions
            val scaledImageWidth = imageWidth * scaleFactor
            val scaledImageHeight = imageHeight * scaleFactor

            // Calculate offsets to center the image on the canvas
            val offsetX = (width - scaledImageWidth) / 2f
            val offsetY = (height - scaledImageHeight) / 2f

            // Iterate through each detected face
            faceLandmarkerResult.faceLandmarks().forEach { faceLandmarks ->
                // Draw all landmarks for the current face
                drawFaceLandmarks(canvas, faceLandmarks, offsetX, offsetY)
//                drawRawAngleData(canvas, faceLandmarks, offsetX, offsetY)
                // Draw all connectors for the current face
//                drawFaceConnectors(canvas, faceLandmarks, offsetX, offsetY)
            }
        }

    }
//    private fun drawRawAngleData(
//        canvas: Canvas,
//        faceLandmarks: List<NormalizedLandmark>,
//        offsetX: Float,
//        offsetY: Float
//    ) {
//        val df = DecimalFormat("#.##")
//
//        // 미간
//        val glabella = faceLandmarks[9]
//        val glabellaX = glabella.x() * imageWidth * scaleFactor + offsetX
//        val glabellaY = glabella.y() * imageHeight * scaleFactor + offsetY
//        canvas.drawLine(glabellaX , glabellaY - 300, glabellaX, glabellaY + 800 , axisPaint)
//
//        // 눈동자
//        val leftEye = faceLandmarks[468]
//        val rightEye = faceLandmarks[473]
//        val leftEyeX = leftEye.x() * imageWidth * scaleFactor + offsetX
//        val leftEyeY = leftEye.y() * imageHeight * scaleFactor + offsetY
//        val rightEyeX = rightEye.x() * imageWidth * scaleFactor + offsetX
//        val rightEyeY = rightEye.y() * imageHeight * scaleFactor + offsetY
//        canvas.drawLine(leftEyeX - 300, leftEyeY, rightEyeX + 300 , rightEyeY , axisPaint)
//        val eyeAngle = calculateSlope(leftEye.x(), leftEye.y(), rightEye.x(), rightEye.y())
//        canvas.drawText(df.format(eyeAngle), rightEyeX + 300, rightEyeY, textPaint)
//
//        // 귓볼 혹은 광대뼈
//        val leftEarLobe = faceLandmarks[93] //234
//        val rightEarLobe = faceLandmarks[323] // 454
//        val leftEarLobeX = leftEarLobe.x() * imageWidth * scaleFactor + offsetX
//        val leftEarLobeY = leftEarLobe.y() * imageHeight * scaleFactor + offsetY
//        val rightEarLobeX = rightEarLobe.x() * imageWidth * scaleFactor + offsetX
//        val rightEarLobeY = rightEarLobe.y() * imageHeight * scaleFactor + offsetY
//
//        canvas.drawLine(leftEarLobeX - 200, leftEarLobeY, rightEarLobeX + 200 , rightEarLobeY , axisPaint)
//
//        // 양쪽 광대뼈 각도
//        val earLobeAngle = calculateSlope(leftEarLobe.x(), leftEarLobe.y(), rightEarLobe.x(), rightEarLobe.y())
//        canvas.drawText(df.format(earLobeAngle), rightEarLobeX + 200, rightEarLobeY, textPaint)
//
//        // 양쪽 광대뼈 중심축에서 거리
//        val leftEarLobeDistance = getRealDistanceX(Pair(leftEarLobe.x(), leftEarLobe.y()), Pair(glabella.x() , glabella.y()))
//        val rightEarLobeDistance = getRealDistanceX(Pair(rightEarLobe.x(), rightEarLobe.y()), Pair(glabella.x() , glabella.y()))
//        canvas.drawText(df.format(leftEarLobeDistance), leftEarLobeX, leftEarLobeY, textPaint)
//        canvas.drawText(df.format(rightEarLobeDistance), rightEarLobeX , rightEarLobeY, textPaint)
//
//        val leftOralAngle = faceLandmarks[61]
//        val rightOralAngle = faceLandmarks[291]
//        val leftOralAngleX = leftOralAngle.x() * imageWidth * scaleFactor + offsetX
//        val leftOralAngleY = leftOralAngle.y() * imageHeight * scaleFactor + offsetY
//        val rightOralAngleX = rightOralAngle.x() * imageWidth * scaleFactor + offsetX
//        val rightOralAngleY = rightOralAngle.y() * imageHeight * scaleFactor + offsetY
//        canvas.drawLine(leftOralAngleX - 300, leftOralAngleY, rightOralAngleX + 300 , rightOralAngleY , axisPaint)
//        val oralAngle = calculateSlope(leftOralAngle.x(), leftOralAngle.y(), rightOralAngle.x(), rightOralAngle.y())
//        canvas.drawText(df.format(oralAngle), rightOralAngleX + 300,  rightOralAngleY, textPaint)
//
//        // 양쪽 입술 끝 거리 ( 미간 -
//        val lipMountain = faceLandmarks[0]
//
//        val leftOralAngleDistance = getRealDistanceX(Pair(leftOralAngle.x(), leftOralAngle.y()), Pair(lipMountain.x() , lipMountain.y()))
//        val rightOralAngleDistance = getRealDistanceX(Pair(leftOralAngle.x(), leftOralAngle.y()), Pair(lipMountain.x() , lipMountain.y()))
//        canvas.drawText(df.format(leftOralAngleDistance), leftOralAngleX, leftOralAngleY, textPaint)
//        canvas.drawText(df.format(rightOralAngleDistance), rightOralAngleX, rightOralAngleY, textPaint)
//
//        // 수직 각도
//        val endNoseCenter = faceLandmarks[168]
//        val bottomRipCenter = faceLandmarks[17]
//        val endNoseCenterX = endNoseCenter.x() * imageWidth * scaleFactor + offsetX
//        val endNoseCenterY = endNoseCenter.y() * imageHeight * scaleFactor + offsetY
//        val bottomRipCenterX = bottomRipCenter.x() * imageWidth * scaleFactor + offsetX
//        val bottomRipCenterY = bottomRipCenter.y() * imageHeight * scaleFactor + offsetY
//        canvas.drawLine(endNoseCenterX , endNoseCenterY, bottomRipCenterX , bottomRipCenterY + 200, axisSubPaint)
//        val bottomFaceAngle = calculateSlope(endNoseCenter.x(), endNoseCenter.y(), bottomRipCenter.x(), bottomRipCenter.y())
//        canvas.drawText(df.format(bottomFaceAngle), bottomRipCenterX, bottomRipCenterY + 100, textPaint)
//
//        // 삼각형 만들기
//        val leftLips = faceLandmarks[62]
//        val rightLips = faceLandmarks[292]
//        val leftLipsX = leftLips.x() * imageWidth * scaleFactor + offsetX
//        val leftLipsY = leftLips.y() * imageHeight * scaleFactor + offsetY
//
//        val rightLipsX = rightLips.x() * imageWidth * scaleFactor + offsetX
//        val rightLipsY = rightLips.y() * imageHeight * scaleFactor + offsetY
//
//        val centerTopLips = faceLandmarks[13]
//        val centerBottomLips = faceLandmarks[14]
//        val centerTopLipsX = centerTopLips.x() * imageWidth * scaleFactor + offsetX
//        val centerTopLipsY = centerTopLips.y() * imageHeight * scaleFactor + offsetY
//
//        val centerBottomLipsX = centerBottomLips.x() * imageWidth * scaleFactor + offsetX
//        val centerBottomLipsY = centerBottomLips.y() * imageHeight * scaleFactor + offsetY
//        canvas.drawLine(leftLipsX, leftLipsY, centerTopLipsX, centerTopLipsY, linePaint)
//        canvas.drawLine(leftLipsX, leftLipsY, centerBottomLipsX, centerBottomLipsY, linePaint)
//
//        canvas.drawLine(rightLipsX, rightLipsY, centerTopLipsX, centerTopLipsY, linePaint)
//        canvas.drawLine(rightLipsX, rightLipsY, centerBottomLipsX, centerBottomLipsY, linePaint)
//
//        // 입술 위 아래 길이
//        val lipsVerticalDistance = getRealDistanceY(Pair(centerTopLips.x(), centerTopLips.y()), Pair(centerBottomLips.x() , centerBottomLips.y()))
//        canvas.drawText(df.format(lipsVerticalDistance), centerBottomLipsX, centerBottomLipsY + 50, textPaint)
//
//        // 입술 위아래 선
//        canvas.drawLine(centerTopLipsX, centerTopLipsY, centerBottomLipsX, centerBottomLipsY, linePaint)
//
//        // 입술 양끝 가운데 선
//        canvas.drawLine(leftLipsX, leftLipsY, rightLipsX, rightLipsY, linePaint)
//
//    }

    private fun drawFaceLandmarks(
        canvas: Canvas,
        faceLandmarks: List<NormalizedLandmark>,
        offsetX: Float,
        offsetY: Float
    ) {
//        faceLandmarks.forEachIndexed { index, landmark ->
//            val x = landmark.x() * imageWidth * scaleFactor + offsetX
//            val y = landmark.y() * imageHeight * scaleFactor + offsetY
//            canvas.drawCircle(x, y, 3f, circlePaint)
////            canvas.drawText("$index", x, y, anglePaint)
//        }
        // x, y에 들어가있는 좌표는?
        val faceOutLineIndexes = listOf(
            10, 109, 67, 103, 54, 21, 162, 127, 234, 93, 132, 58, 172, 136, 150, 149, 176, 148,
            152, 377, 378, 379, 365, 397, 288, 361, 323, 454, 356, 389, 251, 284, 332, 297, 338, 10
        )
        val outLinePath = Path()
//        Log.v("얼굴테두리각형갯수", "${faceOutLineIndexes.size}")
        faceOutLineIndexes.forEachIndexed { i, landmarkIndex ->
            val landmark = faceLandmarks[landmarkIndex]
            val x = landmark.x() * imageWidth * scaleFactor + offsetX
            val y = landmark.y() * imageHeight * scaleFactor + offsetY

            if (i == 0) {
                outLinePath.moveTo(x, y) // 시작점
            } else {
                outLinePath.lineTo(x, y) // 선 연결
            }
        }
        canvas.drawPath(outLinePath, outLinePaint)


//        val leftLips = faceLandmarks[62]
//        val rightLips = faceLandmarks[292]
//        val leftLipsX = leftLips.x() * imageWidth * scaleFactor + offsetX
//        val leftLipsY = leftLips.y() * imageHeight * scaleFactor + offsetY
//
//        val rightLipsX = rightLips.x() * imageWidth * scaleFactor + offsetX
//        val rightLipsY = rightLips.y() * imageHeight * scaleFactor + offsetY
//
//        val centerTopLips = faceLandmarks[13]
//        val centerBottomLips = faceLandmarks[14]
//        val centerTopLipsX = centerTopLips.x() * imageWidth * scaleFactor + offsetX
//        val centerTopLipsY = centerTopLips.y() * imageHeight * scaleFactor + offsetY
//
//        val centerBottomLipsX = centerBottomLips.x() * imageWidth * scaleFactor + offsetX
//        val centerBottomLipsY = centerBottomLips.y() * imageHeight * scaleFactor + offsetY
//        canvas.drawLine(leftLipsX, leftLipsY, centerTopLipsX, centerTopLipsY, outLinePaint)
//        canvas.drawLine(leftLipsX, leftLipsY, centerBottomLipsX, centerBottomLipsY, outLinePaint)
//
//        canvas.drawLine(rightLipsX, rightLipsY, centerTopLipsX, centerTopLipsY, outLinePaint)
//        canvas.drawLine(rightLipsX, rightLipsY, centerBottomLipsX, centerBottomLipsY, outLinePaint)

        val leftEarLobe = faceLandmarks[234] //234
        val rightEarLobe = faceLandmarks[454] // 454

        val centerBottomNose = faceLandmarks[1]
        val leftEarLobeX = leftEarLobe.x() * imageWidth * scaleFactor + offsetX
        val leftEarLobeY = leftEarLobe.y() * imageHeight * scaleFactor + offsetY
        val rightEarLobeX = rightEarLobe.x() * imageWidth * scaleFactor + offsetX
        val rightEarLobeY = rightEarLobe.y() * imageHeight * scaleFactor + offsetY
        val centerBottomNoseX = centerBottomNose.x() * imageWidth * scaleFactor + offsetX
        val centerBottomNoseY = centerBottomNose.y() * imageHeight * scaleFactor + offsetY

        val horizonLinePaint = if (horizontalLineSuccess) standardPaint else notStandardPaint

        canvas.drawLine(leftEarLobeX, leftEarLobeY, centerBottomNoseX, centerBottomNoseY, horizonLinePaint)
        canvas.drawLine(rightEarLobeX, rightEarLobeY, centerBottomNoseX, centerBottomNoseY, horizonLinePaint)

        // 10 - 1 - 152 이마 - 턱
        val foreheadLineIndexes = listOf(10, 1, 152)
        val foreheadLinePath = Path()

        foreheadLineIndexes.forEachIndexed { i, landmarkIndex ->
            val landmark = faceLandmarks[landmarkIndex]
            val x = landmark.x() * imageWidth * scaleFactor + offsetX
            val y = landmark.y() * imageHeight * scaleFactor + offsetY

            if (i == 0) {
                foreheadLinePath.moveTo(x, y) // 시작점
            } else {
                foreheadLinePath.lineTo(x, y) // 선 연결
            }
        }

        // 각도가 잘 맞는지 판단
        val vertiLinePaint = if (verticalLineSuccess) standardPaint else notStandardPaint

        canvas.drawPath(foreheadLinePath, vertiLinePaint)

//        // 얼굴 내부 수직 선
//        val verticalLineIndexes = listOf(152, 17, 0, 14 , 4)
//        val verticalLinePath = Path()
//
//        verticalLineIndexes.forEachIndexed { i, landmarkIndex ->
//            val landmark = faceLandmarks[landmarkIndex]
//            val x = landmark.x() * imageWidth * scaleFactor + offsetX
//            val y = landmark.y() * imageHeight * scaleFactor + offsetY
//
//            if (i == 0) {
//                verticalLinePath.moveTo(x, y) // 시작점
//            } else {
//                verticalLinePath.lineTo(x, y) // 선 연결
//            }
//        }
//        canvas.drawPath(verticalLinePath, outLinePaint)
    }

//    private fun drawFaceConnectors(
//        canvas: Canvas,
//        faceLandmarks: List<NormalizedLandmark>,
//        offsetX: Float,
//        offsetY: Float
//    ) {
//        FaceLandmarker.FACE_LANDMARKS_CONNECTORS.filterNotNull().forEachIndexed { index, connector ->
//            val startLandmark = faceLandmarks.getOrNull(connector.start())
//            val endLandmark = faceLandmarks.getOrNull(connector.end())
//
//            if (startLandmark != null && endLandmark != null) {
//                val startX = startLandmark.x() * imageWidth * scaleFactor + offsetX
//                val startY = startLandmark.y() * imageHeight * scaleFactor + offsetY
//                val endX = endLandmark.x() * imageWidth * scaleFactor + offsetX
//                val endY = endLandmark.y() * imageHeight * scaleFactor + offsetY
//
//                canvas.drawLine(startX, startY, endX, endY, linePaint)
//            }
//        }
//    }

    fun setResults(
        faceLandmarkerResults: FaceLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = faceLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
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
