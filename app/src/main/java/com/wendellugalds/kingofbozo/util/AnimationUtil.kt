package com.wendellugalds.kingofbozo.util

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import com.wendellugalds.kingofbozo.R

object AnimationUtil {

    fun applyExpansionAnimation(view: View) {
        val context = view.context
        val startHeight = (80 * context.resources.displayMetrics.density).toInt()
        val endHeight = (150 * context.resources.displayMetrics.density).toInt()
        
        view.layoutParams.height = startHeight
        view.setBackgroundResource(R.drawable.background_card_btn_repouso_destaque_menu)
        view.requestLayout()

        val animator = ValueAnimator.ofInt(startHeight, endHeight)
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            val params = view.layoutParams
            params.height = value
            view.layoutParams = params
        }
        
        animator.duration = 700
        animator.interpolator = OvershootInterpolator(1.5f)
        view.setBackgroundResource(R.drawable.background_card_btn_active_destaque_menu)
        animator.start()
    }

    fun applyCollapseAnimation(view: View, onEnd: () -> Unit = {}) {
        val context = view.context
        val startHeight = view.height
        val endHeight = (80 * context.resources.displayMetrics.density).toInt()

        val animator = ValueAnimator.ofInt(startHeight, endHeight)
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            val params = view.layoutParams
            params.height = value
            view.layoutParams = params
        }
        
        animator.duration = 300
        animator.interpolator = AccelerateInterpolator()
        
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: android.animation.Animator) {
                view.setBackgroundResource(R.drawable.background_card_btn_repouso_destaque_menu)
            }
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onEnd()
            }
        })
        
        animator.start()
    }

    fun collapseAnyExpandedButton(root: View, onComplete: () -> Unit) {
        val buttonIds = listOf(
            R.id.button_marcador,
            R.id.button_adicionar_jogador,
            R.id.button_marcador_jogo,
            R.id.card_cor
        )
        var foundAndAnimated = false

        for (id in buttonIds) {
            val view = root.findViewById<View>(id)
            if (view != null && view.isVisible && view.height > (100 * root.context.resources.displayMetrics.density).toInt()) {
                applyCollapseAnimation(view, onEnd = onComplete)
                foundAndAnimated = true
                break
            }
        }

        if (!foundAndAnimated) {
            onComplete()
        }
    }
}
