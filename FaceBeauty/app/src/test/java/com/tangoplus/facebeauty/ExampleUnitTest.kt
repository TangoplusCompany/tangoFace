package com.tangoplus.facebeauty

import org.junit.Test

import org.junit.Assert.*
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }


    @Test
    fun calculateAngle() {
//        val chin1 = Pair(559.2246, 340.30695) // 58 왼쪽 사각턱 점
//        val chin2 = Pair(644.1502,351.1653) // 152
//        val chin3 = Pair(740.2037, 344.6672) // 288 오른쪽 사각턱 점
//        val earFlap1 = Pair(556.297,284.96625)
//        val earFlap2 = Pair(750.1916,291.1114)
//        println("턱 중앙: " + calculateAngle(chin1.first, chin1.second, chin2.first, chin2.second, chin3.second, chin3.second))
//        println("턱 왼쪽: " + calculateAngle(earFlap1.first, earFlap1.second, chin1.first, chin1.second, chin2.second, chin2.second))
//        println("턱 오른쪽: " + calculateAngle(earFlap2.first, earFlap2.second, chin3.first, chin3.second, chin2.second, chin2.second))
        // 결과
        // 턱 중앙: 6.043153527582898
        //턱 왼쪽: 89.95927979725006
        //턱 오른쪽: 101.5209511705131

//        val chin1 = Pair(534.79504, 289.2024) // 58 왼쪽 사각턱 점
//        val chin2 = Pair(626.7436, 297.11832) // 152
//        val chin3 = Pair(722.2081, 289.97626) // 288 오른쪽 사각턱 점
//        val earFlap1 = Pair(528.77045,236.25986) // 234
//        val earFlap2 = Pair(728.8677, 237.04642) // 454
//        println("턱 중앙: " + calculateAngle(chin1.first, chin1.second, chin2.first, chin2.second, chin3.second, chin3.second))
//        println("턱 왼쪽: " + calculateAngle(earFlap1.first, earFlap1.second, chin1.first, chin1.second, chin2.second, chin2.second))
//        println("턱 오른쪽: " + calculateAngle(earFlap2.first, earFlap2.second, chin3.first, chin3.second, chin2.second, chin2.second))
//        // 턱 중앙: 3.705574502807713
//        // 턱 왼쪽: 85.4155140919514
//        // 턱 오른쪽: 98.13378969436341

        val chin1 = Pair(560.13184, 327.5407) // 58 왼쪽 사각턱 점
        val chin2 = Pair(627.47406, 322.58765) // 152
        val chin3 = Pair(708.4629, 335.13) // 288 오른쪽 사각턱 점
        val earFlap1 = Pair(554.329, 293.19363) // 234
        val earFlap2 = Pair(717.7128, 300.40973) // 454
        println("턱 중앙: " + calculateAngle(chin1.first, chin1.second, chin2.first, chin2.second, chin3.second, chin3.second))
        println("턱 왼쪽: " + calculateAngle(earFlap1.first, earFlap1.second, chin1.first, chin1.second, chin2.second, chin2.second))
        println("턱 오른쪽: " + calculateAngle(earFlap2.first, earFlap2.second, chin3.first, chin3.second, chin2.second, chin2.second))
        // 턱 중앙: 1.7499182116758887
        // 턱 왼쪽: 79.21608857457207
        // 턱 오른쪽: 103.05613305559454
    }

    @Test
    fun calculateSideHeadAngle() {
        val chin00 = Pair(534.357, 259.94064)
        val chin01 = Pair(531.6538, 279.92352)
        val chin02 = Pair(540.1704, 290.1008)

        val chin10 = Pair(716.28125, 255.25368)
        val chin11 = Pair(719.7607, 275.3083)
        val chin12 = Pair(711.40234, 286.522)

        println("좌측 함몰: " + calculateAngle(chin00.first, chin00.second, chin01.first, chin01.second, chin02.second, chin02.second))
        println("우측 함몰: " + calculateAngle(chin10.first, chin10.second, chin11.first, chin11.second, chin12.second, chin12.second))
        // 좌측 함몰: 100.11656527602321
        // 우측 함몰: 81.63991900419478
        // TODO 이거 선 그려봐야 함
    }


    @Test
    fun calculateCheekSize() {
        val leftCheek = listOf(
            Pair(546.35944, 295.75433),
            Pair(557.7693, 297.8536),
            Pair(573.1019, 306.97113),
            Pair(564.8581, 332.6677),
            Pair(545.6726, 345.29987),
            Pair(532.1117, 320.8714),
            )
        val rightCheek = listOf(
            Pair(705.2586, 292.5131),
            Pair(693.5222, 295.18817),
            Pair(677.8716, 305.08008),
            Pair(686.4264, 330.89975),
            Pair(706.8309, 342.8121),
            Pair(720.3008, 317.6081),
        )
        println("왼쪽 광대 크기 " + calculatePolygonArea(leftCheek))
        println("오른쪽 광대 크기 " + calculatePolygonArea(rightCheek))
        // 절대좌표일때 / 100 해서 약 0.6 정도 차이남 TODO 이 부분은 1000이 맞는지 100이 맞는지 판단해야 함.
        // 왼쪽 광대 크기 12.410206332969828
        // 오른쪽 광대 크기 13.068035858355142
    }




    fun calculateAngle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3:Double): Double {
        val v1x = x1 - x2
        val v1y = y1 - y2
        val v2x = x3 - x2
        val v2y = y3 - y2
        val dotProduct = v1x * v2x + v1y * v2y
        val magnitude1 = sqrt(v1x * v1x + v1y * v1y)
        val magnitude2 = sqrt(v2x * v2x + v2y * v2y)

        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        val angleRadians = acos(cosTheta)
        return toDegrees(angleRadians)
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
        return (abs(sum) / 2.0) / 100
    }
}