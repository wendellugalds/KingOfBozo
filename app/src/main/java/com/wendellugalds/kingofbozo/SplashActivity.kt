package com.wendellugalds.kingofbozo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
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

        // Delay de 3 segundos (3000ms)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
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
