package com.tangoplus.facebeauty.util

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.gson.Gson
import com.tangoplus.facebeauty.data.DrawLine
import com.tangoplus.facebeauty.data.DrawRatioLine
import com.tangoplus.facebeauty.data.FaceComparisonItem
import com.tangoplus.facebeauty.data.FaceLandmarkResult
import com.tangoplus.facebeauty.data.FaceLandmarkResult.Companion.fromCoordinates
import com.tangoplus.facebeauty.data.FaceResult
import com.tangoplus.facebeauty.data.db.FaceStatic
import com.tangoplus.facebeauty.vm.GalleryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import kotlin.coroutines.resume
import kotlin.math.abs

object FileUtility {

    fun getPathFromUri(context: Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                path = it.getString(columnIndex)
            }
        }
        return path
    }

    fun extractVideoCoordinates(jsonData: JSONArray) : List<List<Pair<Float,Float>>> { // 200개의 33개의 x,y
        return List(jsonData.length()) { i ->
            val landmarks = jsonData.getJSONObject(i).getJSONArray("pose_landmark")
            List(landmarks.length()) { j ->
                val landmark = landmarks.getJSONObject(j)
                Pair(
                    landmark.getDouble("sx").toFloat(),
                    landmark.getDouble("sy").toFloat()
                )
            }
        }
    }
    fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                arrayOf(
                    Manifest.permission.CAMERA,
//                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
//                    Manifest.permission.READ_MEDIA_VIDEO,
//                    Manifest.permission.READ_MEDIA_AUDIO,
//                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
//                    Manifest.permission.POST_NOTIFICATIONS,
//                    Manifest.permission.USE_EXACT_ALARM
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.CAMERA,
//                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
//                    Manifest.permission.READ_MEDIA_VIDEO,
//                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS,
//                    Manifest.permission.SCHEDULE_EXACT_ALARM
                )
            }
            else -> {
                arrayOf(
                    Manifest.permission.CAMERA,
//                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }


    fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }
    fun getPathFromContentUri(context: Context, contentUri: Uri): String? {
        var filePath: String? = null
        var cursor: Cursor? = null
        try {
            // 쿼리할 컬럼을 정의합니다. MediaStore.Images.Media.DATA는 실제 파일 경로를 나타냅니다.
            val projection = arrayOf(MediaStore.Images.Media.DATA)

            // ContentResolver를 사용하여 MediaStore를 쿼리합니다.
            cursor = context.contentResolver.query(contentUri, projection, null, null, null)

            // 커서가 유효하고 첫 번째 결과로 이동할 수 있다면
            if (cursor != null && cursor.moveToFirst()) {
                // _DATA 컬럼의 인덱스를 가져옵니다.
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                // 해당 인덱스의 문자열 값을 가져와 filePath에 저장합니다.
                filePath = cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 예외 처리: 예를 들어, 권한이 없거나 URI가 유효하지 않을 때 발생할 수 있습니다.
        } finally {
            // 커서를 닫아 리소스를 해제합니다.
            cursor?.close()
        }
        return filePath
    }
    suspend fun setImage(fragment: Fragment, faceResult: FaceResult, seq: Int, ssiv: SubsamplingScaleImageView, gvm: GalleryViewModel, isZoomIn: Boolean = false): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val jsonData = faceResult.results.getJSONObject(seq)
            Log.v("제이슨0", "${faceResult.results}")
            Log.v("제이슨1", "seq: $seq, json: $jsonData")
            val coordinates = extractImageCoordinates(jsonData)
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
                            val faceLandmarkResult = fromCoordinates(coordinates)
                            Log.v("jsonData", "${jsonData.getJSONObject("data")}")
                            val cheekAngle = when (seq) {
                                0 -> jsonData.getJSONObject("data").getDouble("resting_nose_chin_vertical_angle")
                                else -> jsonData.getJSONObject("data").getDouble("occlusal_nose_chin_vertical_angle")
                            }
                            val combinedBitmap = combineImageAndOverlay(
                                bitmap,
                                faceLandmarkResult,
                                gvm.currentCheckedLines,
                                gvm.currentCheckedRatioLines,
                                gvm.currentFaceComparision,
                                seq,
                                when {
                                    cheekAngle > 92f -> 2
                                    cheekAngle > 91f -> 1
                                    cheekAngle < 88f -> -1
                                    cheekAngle < 89f -> -2
                                    else -> 0
                                }
                            )

                            isSet = true
                            // 가로비율은 2배로 확대 세로 비율은 그대로 보여주기
                            val upscaledBitmap = if (isZoomIn) upscaleImage(combinedBitmap, 1.3f) else combinedBitmap
                            ssiv.setImage(ImageSource.bitmap(upscaledBitmap))
                            ssiv.maxScale = 3.5f
                            ssiv.minScale = 1f
//                            ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP)
//                            ssiv.setScaleAndCenter(scaleFactorY, PointF(ssiv.sWidth / 2f, ssiv.sHeight / 2f))

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
            color = "#80FA8700".toColorInt()
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
        // x, y에 들어가있는 좌표는?
//        faceLandmarks.landmarks.forEachIndexed { index, landmark ->
//            val x = landmark.x
//            val y = landmark.y
//            canvas.drawCircle(x, y, 3f, circlePaint)
////            canvas.drawText("$index", x, y, anglePaint)
//        }

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
            warningPoint > 0 -> listOf(123, 50, 207, 212, 202, 204, 194, 201, 208, 171, 140, 170, 169, 135, 192, 123,) // 왼쪽
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
        val topLm0 = faceLandmarks.landmarks[10]
        val bottomLm0 = faceLandmarks.landmarks[8]
        val x00 = topLm0.x
        val y00 = topLm0.y
        val x01 = bottomLm0.x
        val y01 = bottomLm0.y
        drawExtendedLine(canvas, x00, y00, x01, y01, -10f, 0f, axis300Paint)

        val topLm1 = faceLandmarks.landmarks[9]
        val bottomLm1 = faceLandmarks.landmarks[1]
        val x10 = topLm1.x
        val y10 = topLm1.y
        val x11 = bottomLm1.x
        val y11 = bottomLm1.y
        drawExtendedLine(canvas, x10, y10, x11, y11, 10f, 10f, axis300Paint)

        val topLm2 = faceLandmarks.landmarks[4]
        val bottomLm2 = faceLandmarks.landmarks[0]
        val x20 = topLm2.x
        val y20 = topLm2.y
        val x21 = bottomLm2.x
        val y21 = bottomLm2.y
        drawExtendedLine(canvas, x20, y20, x21, y21, 0f, 50f, axis300Paint)

        val topLm3 = faceLandmarks.landmarks[0]
        val bottomLm3 = faceLandmarks.landmarks[152]
        val x30 = topLm3.x
        val y30 = topLm3.y
        val x31 = bottomLm3.x
        val y31 = bottomLm3.y
        drawExtendedLine(canvas, x30, y30, x31, y31, 0f, 50f, axis300Paint)
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

            val x00 = glabella.x
            val y00 = glabella.y

            val x10 = subnazale.x
            val y10 = subnazale.y

            val x20 = centerTopLips.x
            val y20 = centerTopLips.y

            val x30 = menton.x
            val y40 = menton.y

            drawExtendedLine(canvas, x00, y00, x00 + 1, y00, 300f, 300f, axis300Paint)
            drawExtendedLine(canvas, x10, y10, x10 + 1, y10, 300f, 300f, axis300Paint)

            drawExtendedLine(canvas, x20, y20, x20 + 1, y20, 300f, 300f, axis300Paint)
            drawExtendedLine(canvas, x30, y40, x30 + 1, y40, 300f, 300f, axis300Paint)
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


    fun extractImageCoordinates(jsonData: JSONObject?): List<Pair<Float, Float>>? {
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
//    fun getImageSizeFromUri(context: Context, uri: Uri): Pair<Int, Int>? {
//        return try {
//            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
//            context.contentResolver.openInputStream(uri)?.use { inputStream ->
//                BitmapFactory.decodeStream(inputStream, null, options)
//            }
//            Pair(options.outWidth, options.outHeight)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        }
//    }


    fun readJsonFromUri(context: Context, uri: Uri): JSONObject {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            JSONObject(jsonString)
        } ?: JSONObject()
    }

    fun getImageUriFromFileName(context: Context, fileName: String): Uri? {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }
    fun getJsonUriFromFileName(context: Context, fileName: String): Uri? {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(fileName, "Documents/TangoPlus/") // 경로 끝에 `/` 포함해야 안정적

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    fun JSONObject?.toFaceStatic() : FaceStatic {
        return Gson().fromJson(this.toString(), FaceStatic::class.java)
    }
    fun FaceStatic?.toJSONObject() : String {
        return Gson().toJson(this)
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
    fun scrollToView(view: View, nsv: NestedScrollView) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val viewTop = location[1]
        val scrollViewLocation = IntArray(2)

        nsv.getLocationInWindow(scrollViewLocation)
        val scrollViewTop = scrollViewLocation[1]
        val scrollY = nsv.scrollY
        val scrollTo = scrollY + viewTop - scrollViewTop
        nsv.smoothScrollTo(0, scrollTo)
    }

    private fun isRestingWorse(fcItem : FaceComparisonItem?) : Boolean {
        Log.v("비교확인", "$fcItem")
        return if (fcItem != null) {
            if (abs(fcItem.restingValue) < abs(fcItem.occlusalValue)) {
                false
            } else {
                true
            }
        } else {
            true
        }
    }
    private fun getAngleLevel(value: Float?, type: DrawLine): Int {
        Log.v("값", "$value $type")
        val absValue = 180f - abs(value ?: 0f)

        return when (type) {
            DrawLine.A_EYE -> {
                when {
                    absValue <= 1.2f -> 0
                    absValue <= 2.2f -> 1
                    else -> 2
                }
            }
            DrawLine.A_EARFLAP -> {
                when {
                    absValue <= 1.2f -> 0
                    absValue <= 2.5f -> 1
                    else -> 2
                }
            }
            DrawLine.A_TIP_OF_LIPS -> {
                when {
                    absValue <= 1.8f -> 0
                    absValue <= 3.5f -> 1
                    else -> 2
                }
            }
            else -> throw IllegalArgumentException("지원하지 않는 type입니다. eye, ear, lip 중 하나를 입력하세요.")
        }
    }

}