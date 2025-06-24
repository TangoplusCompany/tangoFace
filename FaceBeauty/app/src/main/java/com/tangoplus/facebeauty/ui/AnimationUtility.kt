package com.tangoplus.facebeauty.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager

object AnimationUtility {
    fun setAnimation(tv: View, duration : Long, delay: Long, fade: Boolean, callback: () -> Unit) {

        val animator = ObjectAnimator.ofFloat(tv, "alpha", if (fade) 0f else 1f, if (fade) 1f else 0f)
        animator.duration = duration
        animator.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                tv.visibility = if (fade) View.VISIBLE else View.INVISIBLE
                callback()
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            animator.start()
        }, delay)
    }

    fun startBouncingAnimation(bounceAnimator: AnimatorSet?, tv: View) {
        val bounceDistance = -10 * tv.resources.displayMetrics.density

        val upAnim = ObjectAnimator.ofFloat(tv, "translationY", 0f, bounceDistance).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        val downAnim = ObjectAnimator.ofFloat(tv, "translationY", bounceDistance, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        bounceAnimator?.apply {
            playSequentially(upAnim, downAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isCancelled) {
                        start()
                    }
                }
                var isCancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    isCancelled = true
                }
            })
            start()
        }
    }
    fun animateTextViewToTopLeft(cl: ConstraintLayout, tv: View, endVerti: Float = 0.5f, endHorizon: Float = 0.5f) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(cl)

        constraintSet.setVerticalBias(tv.id, endVerti)
        constraintSet.setHorizontalBias(tv.id, endHorizon)

        val transition = ChangeBounds().apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
        }

        // 애니메이션 실행
        TransitionManager.beginDelayedTransition(cl, transition)
        constraintSet.applyTo(cl)
    }
}