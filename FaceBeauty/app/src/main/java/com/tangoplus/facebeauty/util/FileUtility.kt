package com.tangoplus.facebeauty.util

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import androidx.core.widget.NestedScrollView
import com.google.gson.Gson
import com.tangoplus.facebeauty.data.db.FaceStatic
import org.json.JSONObject
import androidx.core.graphics.createBitmap

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

//    fun extractVideoCoordinates(jsonData: JSONArray) : List<List<Pair<Float,Float>>> { // 200개의 33개의 x,y
//        return List(jsonData.length()) { i ->
//            val landmarks = jsonData.getJSONObject(i).getJSONArray("pose_landmark")
//            List(landmarks.length()) { j ->
//                val landmark = landmarks.getJSONObject(j)
//                Pair(
//                    landmark.getDouble("sx").toFloat(),
//                    landmark.getDouble("sy").toFloat()
//                )
//            }
//        }
//    }
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


    fun createMirroredOverlayImage(original: Bitmap, isLeftFlip: Boolean): Bitmap {
        val width = original.width
        val height = original.height
        val halfWidth = width / 2

        // 1. 오른쪽 절반 자르기
        val rightHalf = Bitmap.createBitmap(original, if (isLeftFlip) 0 else halfWidth, 0, halfWidth, height)

        // 2. 좌우 반전
        val matrix = Matrix().apply {
            preScale(-1f, 1f)
        }
        val flipped = Bitmap.createBitmap(rightHalf, 0, 0, rightHalf.width, rightHalf.height, matrix, true)

        // 3. 알파값 적용 (50%)
        val alphaBitmap = createBitmap(flipped.width, flipped.height)
        val canvasAlpha = Canvas(alphaBitmap)
        val paint = Paint().apply { alpha = 128 } // 128 out of 255 = 50%
        canvasAlpha.drawBitmap(flipped, 0f, 0f, paint)

        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val x = if (isLeftFlip) (width - halfWidth).toFloat() else 0f  // 오른쪽 끝 위치
        canvas.drawBitmap(alphaBitmap, x, 0f, null)

        return result
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

//    private fun isRestingWorse(fcItem : FaceComparisonItem?) : Boolean {
//        Log.v("비교확인", "$fcItem")
//        return if (fcItem != null) {
//            if (abs(fcItem.restingValue) < abs(fcItem.occlusalValue)) {
//                false
//            } else {
//                true
//            }
//        } else {
//            true
//        }
//    }

}