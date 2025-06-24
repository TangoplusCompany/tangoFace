package com.tangoplus.facebeauty.ui.view

import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class TypingAnimationHelper {

    suspend fun startTypingAnimation(
        textView: TextView,
        fullText: String,
        typingSpeed: Long = 50,
        lineBreakDelay: Long = 250
    ) = withContext(Dispatchers.Main) {
        textView.text = ""

        fullText.forEachIndexed { index, char ->
            textView.text = fullText.substring(0, index + 1)

            val delay = if (char == '\n') {
                typingSpeed + lineBreakDelay
            } else {
                typingSpeed
            }

            delay(delay)
        }
    }
}