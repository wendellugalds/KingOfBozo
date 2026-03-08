package com.wendellugalds.kingofbozo.ui.settings

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.wendellugalds.kingofbozo.MainActivity
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.ActivityThemeLoadingBinding
import com.wendellugalds.kingofbozo.util.ThemeStorage

class ThemeLoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeStorage.applySettings(this)
        val themeKey = intent.getStringExtra("theme_key") ?: "PADRAO"
        setTheme(ThemeStorage.getTheme(this))
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = ActivityThemeLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO

        val themeColor = getThemeColor(themeKey, isNightMode)
        
        binding.waveView.setWaveColor(themeColor)

        startLoadingAnimation()
    }

    private fun getThemeColor(themeKey: String, isNightMode: Boolean): Int {
        return when (themeKey) {
            "PADRAO" -> ContextCompat.getColor(this, if (isNightMode) R.color.padrao_night else R.color.padrao)
            "VERDE" -> ContextCompat.getColor(this, if (isNightMode) R.color.verde_night else R.color.verde)
            "ROXO" -> ContextCompat.getColor(this, if (isNightMode) R.color.roxo_night else R.color.roxo)
            "Rosa" -> ContextCompat.getColor(this, if (isNightMode) R.color.rosa_night else R.color.rosa)
            "LARANJA" -> ContextCompat.getColor(this, if (isNightMode) R.color.laranja_night else R.color.laranja)
            "VERMELHO" -> ContextCompat.getColor(this, if (isNightMode) R.color.vermelho_night else R.color.vermelho)
            else -> ContextCompat.getColor(this, if (isNightMode) R.color.padrao_night else R.color.padrao)
        }
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
