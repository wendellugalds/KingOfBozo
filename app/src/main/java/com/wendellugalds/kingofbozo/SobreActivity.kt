package com.wendellugalds.kingofbozo

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.databinding.ActivitySobreBinding
import com.wendellugalds.kingofbozo.util.ThemeStorage
import com.wendellugalds.kingofbozo.util.SystemBarUtil

class SobreActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySobreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeStorage.applySettings(this)
        setTheme(ThemeStorage.getTheme(this))
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivitySobreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.versaoApp.text = "Versão: ${BuildConfig.VERSION_NAME}"

        configurarCoresDaBarra()
    }

    private fun configurarCoresDaBarra() {
        SystemBarUtil.applySystemBarColors(window, binding.root, statusBarAttr = R.attr.customBackground, navBarAttr = R.attr.customBackground)
    }
}
