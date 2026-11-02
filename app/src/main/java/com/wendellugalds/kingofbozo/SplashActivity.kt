package com.wendellugalds.kingofbozo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.WindowCompat
import com.wendellugalds.kingofbozo.databinding.ActivitySplashBinding
import com.wendellugalds.kingofbozo.util.ThemeStorage
import com.wendellugalds.kingofbozo.util.SystemBarUtil

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeStorage.applySettings(this)
        setTheme(ThemeStorage.getTheme(this))
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarCoresDaBarra()
        iniciarAnimacaoLogo()
    }

    private fun iniciarAnimacaoLogo() {
        // Suaviza a entrada do fundo (glow/logo de fundo)
        binding.imageLogoFundo.alpha = 0f
        binding.imageLogoFundo.animate()
            .alpha(1f)
            .setDuration(1000L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        val allElements = listOf(
            binding.letterR, binding.letterE, binding.letterI,
            binding.letterD, binding.letterODo,
            binding.letterB, binding.logoDice, binding.letterZ, binding.letterOBzo
        )
        
        val duration = 600L // Transição fluida para cada letra
        val delayBetweenLetters = 70L // Intervalo ritmado para revelar "REI DO BOZÓ"

        allElements.forEachIndexed { index, view ->
            view.scaleX = 0.7f
            view.scaleY = 0.7f
            view.alpha = 0f
            view.visibility = View.VISIBLE

            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(duration)
                .setStartDelay(index * delayBetweenLetters)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Animação da coroa caindo sobre "REI"
        binding.crown.apply {
            alpha = 0f
            translationY = -80f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800L)
                .setStartDelay(allElements.size * delayBetweenLetters + 150L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    irParaMain()
                }
                .start()
        }
    }

    private fun irParaMain() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)

            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                Pair(binding.logoContainer, "logo_main"),
                Pair(binding.imageLogoFundo, "logo_fundo_main")
            )

            startActivity(intent, options.toBundle())

            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 1000)
        }, 800)
    }

    private fun configurarCoresDaBarra() {
        SystemBarUtil.applySystemBarColors(window, binding.root, statusBarAttr = R.attr.colorPrimary, navBarAttr = R.attr.colorPrimary)
    }
}