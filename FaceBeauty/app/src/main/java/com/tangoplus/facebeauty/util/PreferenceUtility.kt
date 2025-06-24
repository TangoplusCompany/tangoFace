package com.tangoplus.facebeauty.util

import android.content.Context

class PreferenceUtility(context: Context) {
    private val prefs = context.getSharedPreferences("temp_server_sn_prefs", Context.MODE_PRIVATE)
    private val KEY_LAST_SN = "last_temp_server_sn"

    fun getNextTempServerSn(): Int {
        val lastSn = prefs.getInt(KEY_LAST_SN, 0)
        val nextSn = lastSn + 1

        // 새로운 SN 저장
        prefs.edit().putInt(KEY_LAST_SN, nextSn).apply()

        return nextSn
    }

    // 앱 초기화 시 마지막 SN 확인
    fun getLastTempServerSn(): Int {
        return prefs.getInt(KEY_LAST_SN, 0)
    }
}