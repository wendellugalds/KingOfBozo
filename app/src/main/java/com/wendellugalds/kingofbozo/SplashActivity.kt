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
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.databinding.ActivitySplashBinding
import com.wendellugalds.kingofbozo.util.ThemeStorage

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
            binding.letterK, binding.letterI, binding.letterN, binding.letterG,
            binding.letterOOf, binding.letterF,
            binding.letterB, binding.logoDice, binding.letterZ, binding.letterOBzo
        )
        
        val duration = 600L // Aumentado para uma transição mais fluida
        val delayBetweenLetters = 70L // Reduzido o intervalo para o texto fluir melhor

        allElements.forEachIndexed { index, view ->
            view.scaleX = 0.7f // Começa um pouco maior para evitar o efeito de "explosão"
            view.scaleY = 0.7f
            view.alpha = 0f
            view.visibility = View.VISIBLE

            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(duration)
                .setStartDelay(index * delayBetweenLetters)
                .setInterpolator(DecelerateInterpolator()) // Curva mais suave
                .start()
        }

        // Animação da coroa (mais suave ao cair)
        binding.crown.apply {
            alpha = 0f
            translationY = -80f // Distância menor para um impacto mais sutil
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
        val window = this.window
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)
        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoFundo
        
        val controller = WindowInsetsControllerCompat(window, binding.root)

        // Como o fundo da Splash usa colorPrimary (que é escuro tanto no modo claro quanto no escuro),
        // mantemos os ícones da barra de status e navegação como claros (isAppearanceLight = false)
        // para garantir o contraste.
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
}
