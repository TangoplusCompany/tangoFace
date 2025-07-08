package com.tangoplus.facebeauty.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
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
    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    fun animateExpand(context: Context, view: View, expand: Boolean, onEnd: () -> Unit) {
        val newWidth = if (expand) dpToPx(context, 280) else 0
        val anim = ValueAnimator.ofInt(view.width, newWidth)
        anim.addUpdateListener {
            val value = it.animatedValue as Int
            view.layoutParams.width = value
            view.requestLayout()
        }
        anim.duration = 300
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd()
            }
        })
        anim.start()
    }



    fun startBouncingAnimation(bounceAnimator: AnimatorSet?, tv: View) {
        val bounceDistance = -10 * tv.resources.displayMetrics.density

        val upAnim = ObjectAnimator.ofFloat(tv, "translationY", 0f, bounceDistance).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
        }

        val downAnim = ObjectAnimator.ofFloat(tv, "translationY", bounceDistance, 0f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
        }

        bounceAnimator?.apply {
            playSequentially(upAnim, downAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!isCancelled) {
                            start()
                        }
                    }, 1000) // 1초 딜레이
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