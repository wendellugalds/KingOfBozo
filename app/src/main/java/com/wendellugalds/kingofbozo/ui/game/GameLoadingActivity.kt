package com.wendellugalds.kingofbozo.ui.game

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.MainActivity
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.ui.settings.WaveView
import com.wendellugalds.kingofbozo.util.ThemeStorage

class GameLoadingActivity : AppCompatActivity() {

    private lateinit var waveView: WaveView
    private lateinit var textPercentage: TextView
    private lateinit var textLoadingLabel: TextView
    private var gameId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeStorage.applySettings(this)
        setTheme(ThemeStorage.getTheme(this))
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContentView(R.layout.activity_game_loading)

        waveView = findViewById(R.id.wave_view)
        textPercentage = findViewById(R.id.text_percentage)
        textLoadingLabel = findViewById(R.id.text_loading_label)

        gameId = intent.getLongExtra("GAME_ID", -1)

        val themeColor = MaterialColors.getColor(this, R.attr.colorPrimary, Color.BLUE)
        waveView.setWaveColor(themeColor)

        startLoadingAnimation()
    }

    private fun startLoadingAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 2000
        animator.interpolator = LinearInterpolator()
        
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            waveView.setProgress(progress)
            
            val percentage = (progress * 100).toInt()
            textPercentage.text = "$percentage%"
            
            // Cor do rótulo "CARREGANDO PARTIDA"
            if (progress > 0.45f) {
                textLoadingLabel.setTextColor(Color.WHITE)
            } else {
                val typedValue = android.util.TypedValue()
                theme.resolveAttribute(R.attr.textFillColor, typedValue, true)
                textLoadingLabel.setTextColor(typedValue.data)
            }
            
            // Cor da porcentagem
            if (progress > 0.55f) {
                textPercentage.setTextColor(Color.WHITE)
            } else {
                val typedValue = android.util.TypedValue()
                theme.resolveAttribute(R.attr.textFillColor, typedValue, true)
                textPercentage.setTextColor(typedValue.data)
            }
        }
        
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                finishLoading()
            }
        })
        
        animator.start()
    }

    private fun finishLoading() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("LOAD_GAME_ID", gameId)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
