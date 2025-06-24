package com.tangoplus.facebeauty.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.tangoplus.facebeauty.data.FaceLandmarkResult
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

object MathHelpers {
    private var SCALE_X = 1f
    private const val SCALE_Y = 38f

    fun setScaleX(scale: Float) {
        SCALE_X = scale
    }

    // ------# 기울기 계산 #------
    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians = atan2(y1 - y2, x1 - x2)
        val degrees = toDegrees(radians.toDouble()).toFloat()
        return  if (degrees > 180) degrees % 180 else degrees
    }
    // ------# 점 3개의 각도 계산 #------
    fun calculateAngle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3:Float): Float {
        val v1x = x1 - x2
        val v1y = y1 - y2
        val v2x = x3 - x2
        val v2y = y3 - y2
        val dotProduct = v1x * v2x + v1y * v2y
        val magnitude1 = sqrt(v1x * v1x + v1y * v1y)
        val magnitude2 = sqrt(v2x * v2x + v2y * v2y)

        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        val angleRadians = acos(cosTheta)
        return toDegrees(angleRadians.toDouble()).toFloat()
    }
    fun calculatePolygonArea(points: List<Pair<Double, Double>>): Double {
        if (points.size < 3) return 0.0  // 다각형이 안 됨

        var sum = 0.0
        val n = points.size

        for (i in 0 until n) {
            val (x1, y1) = points[i]
            val (x2, y2) = points[(i + 1) % n]  // 마지막 점은 첫 번째와 연결
            sum += (x1 * y2) - (x2 * y1)
        }
        return abs(sum) / 2.0
    }



    // ------! 선과 점의 X 각도 !------
    fun calculateAngleBySlope(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
        val x4 = (x1 + x2) / 2
        val y4 = (y1 + y2) / 2
        // 벡터1: (x1, y1) -> (x4, y4)
        val dx1 = x4 - x1
        val dy1 = y4 - y1
        // 벡터2: (x4, y4) -> (x3, y3)
        val dx2 = x3 - x4
        val dy2 = y3 - y4
        // 내적 계산
        val dotProduct = dx1 * dx2 + dy1 * dy2
        // 벡터 크기 계산
        val magnitude1 = sqrt(dx1.pow(2) + dy1.pow(2))
        val magnitude2 = sqrt(dx2.pow(2) + dy2.pow(2))
        // 코사인 값 계산
        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        // 라디안 -> 도 변환
        val angleRadians = acos(cosTheta)
        return toDegrees(angleRadians.toDouble()).toFloat()
    }
    private fun getDistanceX(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
        return abs(point2.first - point1.first)
    }

    // Y축 거리 계산
    private fun getDistanceY(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
        return abs(point2.second - point1.second)
    }

    fun getRealDistanceX(point1: Pair<Float, Float>, point2: Pair<Float, Float>) : Float {
        val normalizedDistance = getDistanceX(point1, point2)
        return normalizedToRealDistance(normalizedDistance,true)
    }

    fun getRealDistanceY(point1: Pair<Float, Float>, point2: Pair<Float, Float>) : Float {
        val normalizedDistance = getDistanceY(point1, point2)
        return normalizedToRealDistance(normalizedDistance,  false)
    }

    private fun normalizedToRealDistance(
        normalizedDistance: Float,
        isXAxis: Boolean = true
    ): Float {
        return if (isXAxis) {
            normalizedDistance * SCALE_X
        } else {
            normalizedDistance * SCALE_Y
        }
    }

    fun calculateScaleFromPart(relativeDistance: Float, realPupilLength: Float = 5.5f): Float {
        if (relativeDistance <= 0f) return 0f // 잘못된 값 방어
        return realPupilLength / relativeDistance
    }

    fun calculateRatios(
        points: List<Pair<Float, Float>>,
        isVertical: Boolean = true
    ): List<Float> {
        if (points.size < 2) return emptyList()

        // 1. 각 구간의 거리 계산
        val distances = mutableListOf<Float>()
        for (i in 0 until points.size - 1) {
            val distance = if (!isVertical) {
                abs(points[i + 1].second - points[i].second) // Y 좌표 차이
            } else {
                abs(points[i + 1].first - points[i].first)   // X 좌표 차이
            }
            distances.add(distance)
        }

        // 2. 가장 작은 거리를 1로 기준 설정
        val minDistance = distances.minOrNull() ?: 1f
        if (minDistance == 0f) return distances.map { 1f } // 모든 점이 같은 위치면 1로 반환

        // 3. 비율 계산 (가장 작은 값을 1로)
        return distances.map { it / minDistance }
    }
    fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }


}