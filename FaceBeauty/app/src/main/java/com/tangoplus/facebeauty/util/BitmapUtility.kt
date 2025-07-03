package com.tangoplus.facebeauty.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.tangoplus.facebeauty.data.DrawLine
import com.tangoplus.facebeauty.data.DrawRatioLine
import com.tangoplus.facebeauty.data.FaceComparisonItem
import com.tangoplus.facebeauty.data.FaceLandmarkResult
import com.tangoplus.facebeauty.data.FaceLandmarkResult.Companion.fromFaceCoordinates
import com.tangoplus.facebeauty.data.FaceResult
import com.tangoplus.facebeauty.util.FileUtility.createMirroredOverlayImage
import com.tangoplus.facebeauty.util.FileUtility.getPathFromContentUri
import com.tangoplus.facebeauty.vision.pose.PoseLandmarkResult
import com.tangoplus.facebeauty.vision.pose.PoseLandmarkResult.Companion.fromPoseCoordinates
import com.tangoplus.facebeauty.vm.InformationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

object BitmapUtility {
    suspend fun setImage(fragment: Fragment, faceResult: FaceResult, seq: Int, ssiv: SubsamplingScaleImageView, ivm: InformationViewModel, isZoomIn: Boolean = false): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val jsonData = faceResult.results.getJSONObject(seq)
            Log.v("제이슨0", "${faceResult.results}")
            Log.v("제이슨1", "seq: $seq, json: $jsonData")
            val faceCoordinates = extractFaceCoordinates(jsonData)
            val poseCoordinates = extractPoseCoordinates(jsonData)
            val imageUri = faceResult.imageUris.get(seq)
            var isSet = false

            if (imageUri != null) {
                val path = getPathFromContentUri(fragment.requireContext(), imageUri)
                Log.v("imageUri", "$imageUri , $path")
            }
            val bitmap = BitmapFactory.decodeStream(
                imageUri?.let { fragment.requireContext().contentResolver.openInputStream(it) }
            )
            fragment.lifecycleScope.launch(Dispatchers.Main) {
                imageUri?.let { ImageSource.uri(it) }?.let { ssiv.setImage(it) }
                ssiv.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
                    override fun onReady() {
                        if (!isSet) {
                            val faceLandmarkResult = fromFaceCoordinates(faceCoordinates)
                            val poseLandmarkResult = fromPoseCoordinates(poseCoordinates)
                            Log.v("jsonData", "${jsonData.getJSONObject("data")}")
                            val leftCheekValue = when (seq) {
                                0 -> jsonData.getJSONObject("data").getDouble("resting_left_cheeks_extent")
                                1 -> jsonData.getJSONObject("data").getDouble("occlusal_left_cheeks_extent")
                                2 -> jsonData.getJSONObject("data").getDouble("jaw_left_tilt_left_mandibular_distance")
                                3 -> jsonData.getJSONObject("data").getDouble("jaw_right_tilt_left_mandibular_distance")
                                else -> 0.0
                            }
                            val rightCheekValue = when (seq) {
                                0 -> jsonData.getJSONObject("data").getDouble("resting_right_cheeks_extent")
                                1 -> jsonData.getJSONObject("data").getDouble("occlusal_right_cheeks_extent")
                                2 -> jsonData.getJSONObject("data").getDouble("jaw_left_tilt_right_mandibular_distance")
                                3 -> jsonData.getJSONObject("data").getDouble("jaw_right_tilt_right_mandibular_distance")
                                else -> 0.0
                            }
                            val maxValue = max(leftCheekValue, rightCheekValue)
                            val cheekDifferenceRatio = if (maxValue != 0.0) {
                                abs(leftCheekValue - rightCheekValue) / maxValue
                            } else {
                                0.0 // 둘 다 0인 경우
                            }.toFloat()
                            val cheeksState = when {
                                cheekDifferenceRatio > 0.2 && leftCheekValue > rightCheekValue -> -2
                                cheekDifferenceRatio > 0.1 && leftCheekValue > rightCheekValue -> -1
                                cheekDifferenceRatio > 0.2 && leftCheekValue < rightCheekValue -> 2
                                cheekDifferenceRatio > 0.1 && leftCheekValue < rightCheekValue -> 1
                                cheekDifferenceRatio <= 0.1 -> 0
                                else -> 0
                            }
                            val combinedBitmap = combineImageAndOverlay(
                                bitmap,
                                faceLandmarkResult,
                                poseLandmarkResult,
                                ivm.currentCheckedLines,
                                ivm.currentCheckedRatioLines,
                                ivm.currentFaceComparision,
                                seq,
                                cheeksState
                            )

                            isSet = true
                            // 가로비율은 2배로 확대 세로 비율은 그대로 보여주기
                            val upscaledBitmap = if (isZoomIn) upscaleImage(combinedBitmap, 1.3f) else combinedBitmap
                            val isMirrored = createMirroredOverlayImage(upscaledBitmap, false)

//                            val moareBitmap = setMoare(faceResult, seq, upscaledBitmap)
                            ssiv.setImage(ImageSource.bitmap(upscaledBitmap))
                            ssiv.maxScale = 3.5f
                            ssiv.minScale = 1f
                            continuation.resume(true)
                        }
                    }
                    override fun onImageLoaded() {  }
                    override fun onPreviewLoadError(e: Exception?) { continuation.resume(false) }
                    override fun onImageLoadError(e: Exception?) { continuation.resume(false) }
                    override fun onTileLoadError(e: Exception?) { continuation.resume(false) }
                    override fun onPreviewReleased() { continuation.resume(false) }
                })

            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("scalingError", "IndexOutOfBound: ${e.printStackTrace()}")
        } catch (e: FileNotFoundException) {
            Log.e("scalingError", "FileNotFound: ${e.printStackTrace()}")
        } catch (e: IllegalStateException) {
            Log.e("scalingError", "IllegalState: ${e.printStackTrace()}" )
        } catch (e: ClassNotFoundException) {
            Log.e("scalingError", "Class Not Found: ${e.printStackTrace()}" )
        } catch (e: Exception) {
            Log.e("scalingError", "Exception: ${e.printStackTrace()}" )
        }
    }

    private val strokeWidthh = 5.5f
    fun combineImageAndOverlay (
        originalBitmap: Bitmap,
        faceLandmarks: FaceLandmarkResult,
        poseLandmarks: PoseLandmarkResult,
        selectedData: MutableSet<DrawLine>,
        selectedRatioLine: MutableSet<DrawRatioLine>,
        cfc : MutableList<FaceComparisonItem>,
        seq: Int,
        warningPoint: Int
    ) : Bitmap {
        val matrix = Matrix().apply {
            preScale(1f, 1f)
        }
        val flippedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        val resultBitmap = flippedBitmap .copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val axisPaint = Paint().apply {
            color = "#80FF2819".toColorInt()
            strokeWidth = strokeWidthh
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        val axisSubPaint = Paint().apply {
            color = "#8000FF00".toColorInt()
            strokeWidth = strokeWidthh
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        val axis100Paint = Paint().apply {
            color = "#802EE88B".toColorInt()
            strokeWidth = strokeWidthh
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        val axis200Paint = Paint().apply {
            color = "#802EE88B".toColorInt()
            strokeWidth = strokeWidthh
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        val axis300Paint = Paint().apply {
            color = "#8000FF00".toColorInt()
            strokeWidth = strokeWidthh
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        val outLinePaint = Paint().apply {
            color = "#80FFFFFF".toColorInt()
            strokeWidth = strokeWidthh
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        val warningPaint = Paint().apply {
            color = "#33F48600".toColorInt()
            strokeWidth = strokeWidthh
            style = Paint.Style.FILL_AND_STROKE

            isAntiAlias = true
            maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        }
        val dangerPaint = Paint().apply {
            color = "#33F40000".toColorInt()
            strokeWidth = strokeWidthh
            style = Paint.Style.FILL_AND_STROKE
            isAntiAlias = true
            maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        }
        val dashedPaint = Paint().apply {
            color = "#33F40000".toColorInt()
            strokeWidth = 4f
            style = Paint.Style.STROKE

            // Dash 설정: [길이, 간격, 길이, 간격, ...]
            pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
        }

        val faceOutLineIndexes = listOf(
            10, 109, 67, 103, 54, 21, 162, 127, 234, 93, 132, 58, 172, 136, 150, 149, 176, 148,
            152, 377, 378, 379, 365, 397, 288, 361, 323, 454, 356, 389, 251, 284, 332, 297, 338, 10
        )
        val outLinePath = Path()

        faceOutLineIndexes.forEachIndexed { i, landmarkIndex ->
            val landmark = faceLandmarks.landmarks[landmarkIndex]
            val x = landmark.x
            val y = landmark.y

            if (i == 0) {
                outLinePath.moveTo(x, y) // 시작점
            } else {
                outLinePath.lineTo(x, y) // 선 연결
            }
        }
        canvas.drawPath(outLinePath, outLinePaint)

        // 어느쪽이 안 좋은지 볼 부위 색 넣기
        val cheekIndexes = when  {
            warningPoint > 0 -> listOf(
                123,
                50,
                207,
                212,
                202,
                204,
                194,
                201,
                208,
                171,
                140,
                170,
                169,
                135,
                192,
                123
            ) // 왼쪽
            warningPoint < 0 -> listOf(352, 280, 427, 432, 422, 424, 418, 421, 428, 396, 369, 395, 394, 364, 416, 352) // 오른쪽
            else  -> listOf()
        }
        val cheekPaint = if (abs(warningPoint) == 1) warningPaint else dangerPaint
        val cheekPath = Path()
        cheekIndexes.forEachIndexed { i, landmarkIndex ->
            val landmark = faceLandmarks.landmarks[landmarkIndex]
            val x = landmark.x
            val y = landmark.y

            if (i == 0) {
                cheekPath.moveTo(x, y) // 시작점
            } else {
                cheekPath.lineTo(x, y) // 선 연결
            }
        }
        canvas.drawPath(cheekPath, cheekPaint)
        // 세로축 그리기
        val topLm00 = faceLandmarks.landmarks[10]
        val bottomLm00 = faceLandmarks.landmarks[8]
        val xx00 = topLm00.x
        val yy00 = topLm00.y
        val xx01 = bottomLm00.x
        val yy01 = bottomLm00.y
        drawExtendedLine(canvas, xx00, yy00, xx01, yy01, -10f, 0f, axis300Paint)

        val topLm11 = faceLandmarks.landmarks[9]
        val bottomLm11 = faceLandmarks.landmarks[1]
        val xx10 = topLm11.x
        val yy10 = topLm11.y
        val xx11 = bottomLm11.x
        val yy11 = bottomLm11.y
        drawExtendedLine(canvas, xx10, yy10, xx11, yy11, 10f, 10f, axis300Paint)

        val topLm22 = faceLandmarks.landmarks[4]
        val bottomLm22 = faceLandmarks.landmarks[0]
        val xx20 = topLm22.x
        val yy20 = topLm22.y
        val xx21 = bottomLm22.x
        val yy21 = bottomLm22.y
        drawExtendedLine(canvas, xx20, yy20, xx21, yy21, 0f, 50f, axis300Paint)

        val topLm3 = faceLandmarks.landmarks[0]
        val bottomLm3 = faceLandmarks.landmarks[152]
        val x30 = topLm3.x
        val y30 = topLm3.y
        val x31 = bottomLm3.x
        val y31 = bottomLm3.y
        drawExtendedLine(canvas, x30, y30, x31, y31, 0f, 50f, axis300Paint)

        // 목젖
        val midShoulderX = (poseLandmarks.landmarks[11].x + poseLandmarks.landmarks[12].x ) / 2
        val midShoulderY = (poseLandmarks.landmarks[11].y + poseLandmarks.landmarks[12].y ) / 2
        drawExtendedLine(canvas, midShoulderX, midShoulderY, midShoulderX, midShoulderY + 1, 500f, 500f, dashedPaint)

//        val faceplr = listOf(7, 8, 0, 11, 12, )
        val circlePaint1 = Paint().apply {
            color = "#80F40000".toColorInt()
            style = Paint.Style.FILL
        }
        val circlePaint2 = Paint().apply {
            color = "#80FF4141".toColorInt()
            style = Paint.Style.FILL
        }
        val circlePaint3 = Paint().apply {
            color = "#80FF7979".toColorInt()
            style = Paint.Style.FILL
        }


//        faceplr.forEach { indexx ->
//            val plr =  poseLandmarks.landmarks[indexx]
//
//            canvas.drawCircle(plr.x, plr.y, 12f, circlePaint)
//        }
        val facePlr1 = listOf(132, 361)
        val facePlr2 = listOf(93, 323)
        val facePlr3 = listOf(454, 234)
        facePlr1.forEach { indexx ->
            val plr =  faceLandmarks.landmarks[indexx]
            canvas.drawCircle(plr.x, plr.y, 12f, circlePaint1)
        }
        facePlr2.forEach { indexx ->
            val plr =  faceLandmarks.landmarks[indexx]
            canvas.drawCircle(plr.x, plr.y, 12f, circlePaint2)
        }
        facePlr3.forEach { indexx ->
            val plr =  faceLandmarks.landmarks[indexx]
            canvas.drawCircle(plr.x, plr.y, 12f, circlePaint3)
        }

        // ----------------------------------# 비율 계산기 #------------------------------------
        if (selectedRatioLine.contains(DrawRatioLine.A_VERTI)) {
            Log.v("비율Ratio", "${selectedRatioLine}")
            val leftFace = listOf(21, 162, 127, 234, 93, 132, 58)
            val rightFace = listOf(251, 390, 356, 454, 323, 361)
            val minLeftIndex = leftFace.minByOrNull { index -> faceLandmarks.landmarks[index].x }
            val minRightIndex = rightFace.maxByOrNull { index -> faceLandmarks.landmarks[index].x }
            val leftLm = minLeftIndex?.let { faceLandmarks.landmarks.get(it) }
            val rightLm = minRightIndex?.let { faceLandmarks.landmarks.get(it) }

            val x0 = leftLm?.x ?: 0f
            val y0 = leftLm?.y ?: 0f

            val x1 = rightLm?.x ?: 0f
            val y1 = rightLm?.y ?: 0f
            drawExtendedLine(canvas, x0, y0, x0, y0 + 1, 400f, 400f, axis300Paint)
            drawExtendedLine(canvas, x1, y1, x1, y1 + 1, 400f, 400f, axis300Paint)

            val leftEyeStart = faceLandmarks.landmarks[33]
            val leftEyeEnd = faceLandmarks.landmarks[133]

            val rightEyeStart = faceLandmarks.landmarks[362]
            val rightEyeEnd = faceLandmarks.landmarks[263]

            val x2 = leftEyeStart.x
            val y2 = leftEyeStart.y
            val x3 = leftEyeEnd.x
            val y3 = leftEyeEnd.y
            val x4 = rightEyeStart.x
            val y4 = rightEyeStart.y
            val x5 = rightEyeEnd.x
            val y5 = rightEyeEnd.y

            drawExtendedLine(canvas, x2, y2, x2, y2 + 1, 400f, 400f, axis300Paint)
            drawExtendedLine(canvas, x3, y3, x3, y3 + 1, 400f, 400f, axis300Paint)
            drawExtendedLine(canvas, x4, y4, x4, y4 + 1, 400f, 400f, axis300Paint)
            drawExtendedLine(canvas, x5, y5, x5, y5 + 1, 400f, 400f, axis300Paint)

        }
        if (selectedRatioLine.contains(DrawRatioLine.A_HORIZON)) {
            Log.v("비율Ratio", "${selectedRatioLine}")
            val glabella = faceLandmarks.landmarks[8]
            val subnazale = faceLandmarks.landmarks[2]
            val centerTopLips = faceLandmarks.landmarks[13]
            val menton = faceLandmarks.landmarks[152]

            val x0 = glabella.x
            val y0 = glabella.y

            val x1 = subnazale.x
            val y1 = subnazale.y

            val x2 = centerTopLips.x
            val y2 = centerTopLips.y

            val x3 = menton.x
            val y4 = menton.y

            drawExtendedLine(canvas, x0, y0, x0 + 1, y0, 300f, 300f, axis300Paint)
            drawExtendedLine(canvas, x1, y1, x1 + 1, y1, 300f, 300f, axis300Paint)

            drawExtendedLine(canvas, x2, y2, x2 + 1, y2, 300f, 300f, axis300Paint)
            drawExtendedLine(canvas, x3, y4, x3 + 1, y4, 300f, 300f, axis300Paint)
        }

        // ------------------------------# 선택에 따라 바로 그리기 #----------------------------------
        if (selectedData.contains(DrawLine.A_EYE)) {
            val leftLm = faceLandmarks.landmarks[468]
            val rightLm = faceLandmarks.landmarks[473]
            val x0 = leftLm.x
            val y0 = leftLm.y
            val x1 = rightLm.x
            val y1 = rightLm.y


            val cfcItem = cfc.find { it.label == "눈 수평 각도" }
            val restValue = cfcItem?.restingValue
            val occlusalValue = cfcItem?.occlusalValue
            val riskLevel = when (seq) {
                0 -> {
                    getAngleLevel(restValue, DrawLine.A_EYE)
                }
                else -> {
                    getAngleLevel(occlusalValue, DrawLine.A_EYE)
                }
            }
            val paintt = when (riskLevel) {
                0 -> axis300Paint
                1 -> axisSubPaint
                else -> axisPaint
            }
            drawExtendedLine(canvas, x0, y0, x1, y1, 250f, 250f, paintt)
        }

        if (selectedData.contains(DrawLine.A_EARFLAP)) {
            val leftLm = faceLandmarks.landmarks[454]
            val rightLm = faceLandmarks.landmarks[234]
            val x0 = leftLm.x
            val y0 = leftLm.y
            val x1 = rightLm.x
            val y1 = rightLm.y

            val centerX = (leftLm.x + rightLm.x) / 2
            val centerY = (leftLm.y + rightLm.y) / 2

            val cfcItem = cfc.find { it.label == "귓바퀴 수평 각도" }
            val restValue = cfcItem?.restingValue
            val occlusalValue = cfcItem?.occlusalValue
            val riskLevel = when (seq) {
                0 -> {
                    getAngleLevel(restValue, DrawLine.A_EARFLAP)
                }
                else -> {
                    getAngleLevel(occlusalValue, DrawLine.A_EARFLAP)
                }
            }
            val paintt = when (riskLevel) {
                0 -> axis300Paint
                1 -> axisSubPaint
                else -> axisPaint
            }

            drawExtendedLine(canvas, centerX, centerY, x0, y0, 0f, 170f, paintt)
            drawExtendedLine(canvas, centerX, centerY, x1, y1, 0f, 170f, paintt)
        }

        if (selectedData.contains(DrawLine.A_TIP_OF_LIPS)) {
            val leftLm = faceLandmarks.landmarks[291]
            val rightLm = faceLandmarks.landmarks[61]
            val x0 = leftLm.x
            val y0 = leftLm.y
            val x1 = rightLm.x
            val y1 = rightLm.y
            val centerX = (leftLm.x + rightLm.x) / 2
            val centerY = (leftLm.y + rightLm.y) / 2
            // 어느 곳이 안좋은지 판단해서 넣기

            val cfcItem = cfc.find { it.label == "입술 끝 수평 각도" }
            val restValue = cfcItem?.restingValue
            val occlusalValue = cfcItem?.occlusalValue
            val riskLevel = when (seq) {
                0 -> {
                    getAngleLevel(restValue, DrawLine.A_TIP_OF_LIPS)
                }
                else -> {
                    getAngleLevel(occlusalValue, DrawLine.A_TIP_OF_LIPS)
                }
            }
            val paintt = when (riskLevel) {
                0 -> axis300Paint
                1 -> axisSubPaint
                else -> axisPaint
            }
            drawExtendedLine(canvas, centerX, centerY, x0, y0, 0f, 250f, paintt)
            drawExtendedLine(canvas, centerX, centerY, x1, y1, 0f, 250f, paintt)

//            val y3 = (leftLm.y + rightLm.y ) / 2
//            drawExtendedLine(canvas, x0, y3, x1, y3, 250f, 250f, axisPaint)

        }
        if (selectedData.contains(DrawLine.A_NOSE_CHIN)) {
            val topLm0 = faceLandmarks.landmarks[4]
            val bottomLm0 = faceLandmarks.landmarks[0]
            val x00 = topLm0.x
            val y00 = topLm0.y
            val x01 = bottomLm0.x
            val y01 = bottomLm0.y
            drawExtendedLine(canvas, x00, y00, x01, y01, -10f, 0f, axis100Paint)

            val topLm1 = faceLandmarks.landmarks[0]
            val bottomLm1 = faceLandmarks.landmarks[17]
            val x10 = topLm1.x
            val y10 = topLm1.y
            val x11 = bottomLm1.x
            val y11 = bottomLm1.y
            drawExtendedLine(canvas, x10, y10, x11, y11, 10f, 10f, axis200Paint)

            val topLm2 = faceLandmarks.landmarks[17]
            val bottomLm2 = faceLandmarks.landmarks[152]
            val x20 = topLm2.x
            val y20 = topLm2.y
            val x21 = bottomLm2.x
            val y21 = bottomLm2.y
            drawExtendedLine(canvas, x20, y20, x21, y21, 0f, 50f, axis300Paint)

//            drawExtendedLine(canvas, x00, y00, x00, y21, -10f, 100f, axisPaint)
        }
        if (selectedData.contains(DrawLine.A_EARFLAP_NASAL_WING)) {
            // 귓볼 - 코 끝 선 왼쪽
            val leftCheeksIndexes = listOf(234, 4)
            val rightCheeksIndexes = listOf(454, 4)
            val leftCheekLinePath = Path()
            val rightCheekLinePath = Path()
            leftCheeksIndexes.forEachIndexed { i, landmarkIndex ->
                val landmark = faceLandmarks.landmarks[landmarkIndex]
                val x = landmark.x
                val y = landmark.y

                if (i == 0) {
                    leftCheekLinePath.moveTo(x, y) // 시작점
                } else {
                    leftCheekLinePath.lineTo(x, y) // 선 연결
                }
            }
            canvas.drawPath(leftCheekLinePath, outLinePaint)

            // 귓볼 - 코 끝 선 오른쪽
            rightCheeksIndexes.forEachIndexed { i, landmarkIndex ->
                val landmark = faceLandmarks.landmarks[landmarkIndex]
                val x = landmark.x
                val y = landmark.y

                if (i == 0) {
                    rightCheekLinePath.moveTo(x, y) // 시작점
                } else {
                    rightCheekLinePath.lineTo(x, y) // 선 연결
                }
            }
            canvas.drawPath(rightCheekLinePath, outLinePaint)
        }
        if (selectedData.contains(DrawLine.A_GLABELLA_NOSE)) {
            val topLm0 = faceLandmarks.landmarks[10]
            val bottomLm0 = faceLandmarks.landmarks[9]
            val x00 = topLm0.x
            val y00 = topLm0.y
            val x01 = bottomLm0.x
            val y01 = bottomLm0.y
            drawExtendedLine(canvas, x00, y00, x01, y01, 30f, 0f, axis300Paint)

            val topLm1 = faceLandmarks.landmarks[9]
            val bottomLm1 = faceLandmarks.landmarks[8]
            val x10 = topLm1.x
            val y10 = topLm1.y
            val x11 = bottomLm1.x
            val y11 = bottomLm1.y
            drawExtendedLine(canvas, x10, y10, x11, y11, 0f, 0f, axis200Paint)

            val topLm2 = faceLandmarks.landmarks[8]
            val bottomLm2 = faceLandmarks.landmarks[4]
            val x20 = topLm2.x
            val y20 = topLm2.y
            val x21 = bottomLm2.x
            val y21 = bottomLm2.y
            drawExtendedLine(canvas, x20, y20, x21, y21, 30f, 0f, axis100Paint)
        }

        if (selectedData.contains(DrawLine.D_EARFLAP_NOSE)) {
            // 귓볼 - 코 끝 선 왼쪽
            val leftCheeksIndexes = listOf(234, 64)
            val rightCheeksIndexes = listOf(454, 294)
            val leftCheekLinePath = Path()
            val rightCheekLinePath = Path()
            leftCheeksIndexes.forEachIndexed { i, landmarkIndex ->
                val landmark = faceLandmarks.landmarks[landmarkIndex]
                val x = landmark.x
                val y = landmark.y

                if (i == 0) {
                    leftCheekLinePath.moveTo(x, y) // 시작점
                } else {
                    leftCheekLinePath.lineTo(x, y) // 선 연결
                }
            }
            canvas.drawPath(leftCheekLinePath, axis300Paint)

            // 귓볼 - 코 끝 선 오른쪽
            rightCheeksIndexes.forEachIndexed { i, landmarkIndex ->
                val landmark = faceLandmarks.landmarks[landmarkIndex]
                val x = landmark.x
                val y = landmark.y

                if (i == 0) {
                    rightCheekLinePath.moveTo(x, y) // 시작점
                } else {
                    rightCheekLinePath.lineTo(x, y) // 선 연결
                }
            }
            canvas.drawPath(rightCheekLinePath, axis300Paint)
        }
        if (selectedData.contains(DrawLine.D_TIP_OF_LIPS_CENTER_LIPS)) {
            val leftLm0 = faceLandmarks.landmarks[61]
            val rightLm1 = faceLandmarks.landmarks[291]
            val centerTopLm = faceLandmarks.landmarks[13]
            val centerBottomLm = faceLandmarks.landmarks[14]
            val x00 = leftLm0.x
            val y00 = leftLm0.y
            val x10 = rightLm1.x
            val y10 = rightLm1.y
            val x01 = centerTopLm.x
            val y01 = centerTopLm.y
            val x02 = centerBottomLm.x
            val y02 = centerBottomLm.y
            drawExtendedLine(canvas, x00, y00, x02, y02, 10f, 0f, axis300Paint)
            drawExtendedLine(canvas, x10, y10, x02, y02, 10f, 0f, axis300Paint)
            drawExtendedLine(canvas, x00, y00, x01, y01, 10f, 0f, outLinePaint)
            drawExtendedLine(canvas, x10, y10, x01, y01, 10f, 0f, outLinePaint)

//            drawExtendedLine(canvas, x01, y01, x01, y02, 20f, 20f, axisSubPaint)
            drawExtendedLine(canvas, x01, y01, x02, y02, 20f, 20f, axis300Paint)
        }

        if (selectedData.contains(DrawLine.D_EARFLAP_NOSE) && selectedData.contains(DrawLine.A_EARFLAP_NASAL_WING)) {
            // 귓볼 - 코 끝 선 왼쪽
            val leftCheeksIndexes = listOf(4, 64)
            val rightCheeksIndexes = listOf(4, 294)
            val middleCheeksIndexes = listOf(64, 294)
            val leftCheekLinePath = Path()
            val rightCheekLinePath = Path()
            val middleCheekLinePath = Path()
            leftCheeksIndexes.forEachIndexed { i, landmarkIndex ->
                val landmark = faceLandmarks.landmarks[landmarkIndex]
                val x = landmark.x
                val y = landmark.y

                if (i == 0) {
                    leftCheekLinePath.moveTo(x, y) // 시작점
                } else {
                    leftCheekLinePath.lineTo(x, y) // 선 연결
                }
            }
            canvas.drawPath(leftCheekLinePath, outLinePaint)

            // 귓볼 - 코 끝 선 오른쪽
            rightCheeksIndexes.forEachIndexed { i, landmarkIndex ->
                val landmark = faceLandmarks.landmarks[landmarkIndex]
                val x = landmark.x
                val y = landmark.y

                if (i == 0) {
                    rightCheekLinePath.moveTo(x, y) // 시작점
                } else {
                    rightCheekLinePath.lineTo(x, y) // 선 연결
                }
            }
            canvas.drawPath(rightCheekLinePath, outLinePaint)
            // 귓볼 - 코 끝 선 오른쪽
            middleCheeksIndexes.forEachIndexed { i, landmarkIndex ->
                val landmark = faceLandmarks.landmarks[landmarkIndex]
                val x = landmark.x
                val y = landmark.y

                if (i == 0) {
                    middleCheekLinePath.moveTo(x, y) // 시작점
                } else {
                    middleCheekLinePath.lineTo(x, y) // 선 연결
                }
            }

            canvas.drawPath(middleCheekLinePath, outLinePaint)
        }
        return resultBitmap
    }
    fun drawExtendedLine(
        canvas: Canvas,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        startExtension: Float,
        endExtension: Float,
        paint: Paint
    ) {
        val dx = x1 - x0
        val dy = y1 - y0
        val length = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

        if (length == 0f) return  // 두 점이 같을 경우는 무시

        val nx = dx / length
        val ny = dy / length

        val extendedStartX = x0 - nx * startExtension
        val extendedStartY = y0 - ny * startExtension
        val extendedEndX = x1 + nx * endExtension
        val extendedEndY = y1 + ny * endExtension

        canvas.drawLine(extendedStartX, extendedStartY, extendedEndX, extendedEndY, paint)
    }


    fun extractFaceCoordinates(jsonData: JSONObject?): List<Pair<Float, Float>>? {
        val poseData = jsonData?.optJSONArray("face_landmark")
        return if (poseData != null) {
            List(poseData.length()) { i ->
                val landmark = poseData.getJSONObject(i)
                Pair(
                    landmark.getDouble("sx").toFloat(),
                    landmark.getDouble("sy").toFloat()
                )
            }
        } else null
    }
    fun extractPoseCoordinates(jsonData: JSONObject?): List<Pair<Float, Float>>? {
        val poseData = jsonData?.optJSONArray("pose_landmark")
        return if (poseData != null) {
            List(poseData.length()) { i ->
                val landmark = poseData.getJSONObject(i)
                Pair(
                    landmark.getDouble("sx").toFloat(),
                    landmark.getDouble("sy").toFloat()
                )
            }
        } else null
    }

    fun extractAllImageCoordinates(jsonData: JSONObject?): List<Triple<Float, Float, Float>>? {
        val poseData = jsonData?.optJSONArray("face_landmark")
        return if (poseData != null) {
            List(poseData.length()) { i ->
                val landmark = poseData.getJSONObject(i)
                Triple(
                    landmark.getDouble("sx").toFloat(),
                    landmark.getDouble("sy").toFloat(),
                    landmark.getDouble("wz").toFloat()
                )
            }
        } else null
    }

    private fun upscaleImage(originalBitmap: Bitmap, scaleFactor: Float): Bitmap {
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val width = (originalBitmap.width * scaleFactor).toInt()
        val height = (originalBitmap.height * scaleFactor).toInt()

        val scaledBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)

        val srcRect = Rect(0, 0, originalBitmap.width, originalBitmap.height)
        val dstRect = Rect(0, 0, width, height)

        canvas.drawBitmap(originalBitmap, srcRect, dstRect, paint)

        return scaledBitmap
    }
    private fun getAngleLevel(value: Float?, type: DrawLine): Int {
        Log.v("값", "$value $type")
        val absValue = 180f - abs(value ?: 0f)

        return when (type) {
            DrawLine.A_EYE -> {
                when {
                    absValue <= 1.2f -> 0
                    absValue <= 2.1f -> 1
                    else -> 2
                }
            }
            DrawLine.A_EARFLAP -> {
                when {
                    absValue <= 1.2f -> 0
                    absValue <= 2.25f -> 1
                    else -> 2
                }
            }
            DrawLine.A_TIP_OF_LIPS -> {
                when {
                    absValue <= 1.8f -> 0
                    absValue <= 3.2f -> 1
                    else -> 2
                }
            }
            else -> throw IllegalArgumentException("지원하지 않는 type입니다. eye, ear, lip 중 하나를 입력하세요.")
        }
    }
//    private fun setMoare(faceResult: FaceResult, seq: Int, originalBitmap: Bitmap): Bitmap {
//        val jsonData = faceResult.results.getJSONObject(seq)
//        val coordinates = extractAllImageCoordinates(jsonData)
//
//        val zValues = coordinates?.map { it.third }
//        val minZ = zValues?.minOrNull() ?: 0f
//        val maxZ = zValues?.maxOrNull() ?: 1f
//        val contourLevels = 8
//        val zStep = (maxZ - minZ) / contourLevels
//        val contourMap = mutableMapOf<Int, MutableList<PointF>>()
//
//        if (coordinates != null && coordinates.isNotEmpty()) {
//            val noseIndex = 1
//            val nose = PointF(coordinates[noseIndex].first, coordinates[noseIndex].second)
//
//            for ((x, y, z) in coordinates) {
//                val contourIndex = ((z - minZ) / zStep).toInt().coerceIn(0, contourLevels - 1)
//                contourMap.getOrPut(contourIndex) { mutableListOf() }.add(PointF(x, y))
//            }
//
//            // 각 등고선 그룹 처리
//            contourMap.forEach { (level, pointList) ->
//                if (pointList.size > 3) {
//                    // 평균 반지름으로 스무딩
//                    val avgRadius = pointList.map { p ->
//                        hypot(p.x - nose.x, p.y - nose.y)
//                    }.average().toFloat()
//
//                    pointList.forEachIndexed { index, point ->
//                        val angle = atan2(point.y - nose.y, point.x - nose.x)
//                        val currentRadius = hypot(point.x - nose.x, point.y - nose.y)
//
//                        // 얼굴 외곽 부분 감지 (이마, 턱)
//                        val isOuterArea = isOuterFaceArea(point, nose, coordinates)
//
//                        // 외곽 부분은 덜 스무딩하고, 중심부는 더 스무딩
//                        val smoothingFactor = if (isOuterArea) 0.1f else 0.7f
//                        val smoothRadius = currentRadius * (1 - smoothingFactor) + avgRadius * smoothingFactor
//
//                        pointList[index] = PointF(
//                            nose.x + cos(angle) * smoothRadius,
//                            nose.y + sin(angle) * smoothRadius
//                        )
//                    }
//
//                    pointList.sortBy { p -> atan2(p.y - nose.y, p.x - nose.x) }
//                }
//            }
//        }
//
//        val bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
//        val canvas = Canvas(bitmap)
//        val paint = Paint().apply {
//            style = Paint.Style.STROKE
//            strokeWidth = 3f
//            isAntiAlias = true
//        }
//
//        contourMap.toSortedMap().forEach { (level, points) ->
//            paint.color = Color.HSVToColor(floatArrayOf(
//                240f - level * (240f / contourLevels), 1f, 0.8f
//            ))
//
//            if (points.size > 3 && coordinates != null) {
//                val nose = PointF(coordinates[1].first, coordinates[1].second)
//                drawSegmentedContour(canvas, points, paint, nose)
//            }
//        }
//
//        return bitmap
//    }
    // 얼굴 외곽 부분인지 판단
    private fun isOuterFaceArea(point: PointF, nose: PointF, coordinates: List<Triple<Float, Float, Float>>): Boolean {
        val angle = atan2(point.y - nose.y, point.x - nose.x)
        val degrees = Math.toDegrees(angle.toDouble()).toFloat()

        // 이마 영역 (위쪽)
        val isForehead = degrees > -45 && degrees < 45 && point.y < nose.y

        // 턱 영역 (아래쪽)
        val isChin = degrees > 135 || degrees < -135 && point.y > nose.y

        return isForehead || isChin
    }

    // 구간별로 나눠서 그리기 (직선 방지)
    private fun drawSegmentedContour(canvas: Canvas, points: List<PointF>, paint: Paint, nose: PointF) {
        if (points.size < 4) return

        val path = Path()
        path.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {
            val p1 = points[i - 1]
            val p2 = points[i]

            val distance = hypot(p2.x - p1.x, p2.y - p1.y)

            // 거리가 너무 크면 (외곽 부분) 베지어 곡선으로 부드럽게
            if (distance > 50f) {
                val midX = (p1.x + p2.x) / 2f
                val midY = (p1.y + p2.y) / 2f

                // 코 방향으로 약간 휘게 하는 제어점
                val controlX = midX + (nose.x - midX) * 0.1f
                val controlY = midY + (nose.y - midY) * 0.1f

                path.quadTo(controlX, controlY, p2.x, p2.y)
            } else {
                // 가까운 점들은 일반적인 베지어 곡선
                val controlX = (p1.x + p2.x) / 2f
                val controlY = (p1.y + p2.y) / 2f
                path.quadTo(controlX, controlY, p2.x, p2.y)
            }
        }

        // 마지막과 첫 번째 점 연결
        val lastPoint = points.last()
        val firstPoint = points.first()
        val finalDistance = hypot(firstPoint.x - lastPoint.x, firstPoint.y - lastPoint.y)

        if (finalDistance > 50f) {
            val midX = (lastPoint.x + firstPoint.x) / 2f
            val midY = (lastPoint.y + firstPoint.y) / 2f
            val controlX = midX + (nose.x - midX) * 0.1f
            val controlY = midY + (nose.y - midY) * 0.1f
            path.quadTo(controlX, controlY, firstPoint.x, firstPoint.y)
        } else {
            path.lineTo(firstPoint.x, firstPoint.y)
        }

        canvas.drawPath(path, paint)
    }

}