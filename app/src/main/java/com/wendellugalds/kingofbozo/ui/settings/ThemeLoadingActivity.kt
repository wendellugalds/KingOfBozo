package com.wendellugalds.kingofbozo.ui.settings

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.MainActivity
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.ActivityThemeLoadingBinding
import com.wendellugalds.kingofbozo.util.ThemeStorage

class ThemeLoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeStorage.applySettings(this)
        setTheme(ThemeStorage.getTheme(this))
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = ActivityThemeLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtém a cor primária diretamente do tema atual para evitar inversão
        val themeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.BLUE)
        binding.waveView.setWaveColor(themeColor)

        startLoadingAnimation()
    }

    private fun startLoadingAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 2500
        animator.interpolator = LinearInterpolator()
        
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            binding.waveView.setProgress(progress)
            
            val percentage = (progress * 100).toInt()
            binding.textPercentage.text = "$percentage%"
            
            // Ajusta o contraste do texto conforme o líquido sobe
            if (progress > 0.55f) {
                binding.textPercentage.setTextColor(Color.WHITE)
            }
        }
        
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                goToMainActivity()
            }
        })
        
        animator.start()
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
