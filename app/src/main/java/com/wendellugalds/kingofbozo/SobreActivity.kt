package com.wendellugalds.kingofbozo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.databinding.ActivitySobreBinding
import com.wendellugalds.kingofbozo.util.ThemeStorage

class SobreActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySobreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeStorage.applySettings(this)
        setTheme(ThemeStorage.getTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivitySobreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.versaoApp.text = "Versão: ${BuildConfig.VERSION_NAME}"

        configurarCoresDaBarra()
    }

    private fun configurarCoresDaBarra() {
        val window = this.window
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.background)
        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoFundo

        val controller = WindowInsetsControllerCompat(window, binding.root)

        val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        controller.isAppearanceLightStatusBars = !isNightMode
        controller.isAppearanceLightNavigationBars = !isNightMode
    }
}
