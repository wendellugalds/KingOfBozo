package com.wendellugalds.kingofbozo.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.wendellugalds.kingofbozo.BuildConfig
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentSettingsBinding
import com.wendellugalds.kingofbozo.util.ThemeStorage

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateThemePreview()
        updateVersionDisplay()
        configurarCoresDaBarra()
        updateNightModeDisplay()

        binding.cardCor.setOnClickListener {
            val bottomSheet = ThemeSelectionBottomSheet { themeResId ->
                ThemeStorage.saveTheme(requireContext(), themeResId)
                requireActivity().recreate()
            }
            bottomSheet.show(parentFragmentManager, "ThemeSelection")
        }

        binding.cardTema.setOnClickListener {
            showNightModeDialog()
        }
    }

    private fun showNightModeDialog() {
        val currentMode = ThemeStorage.getNightMode(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_night_mode, null)
        
        val layoutLight = dialogView.findViewById<LinearLayout>(R.id.toogle_tema_claro)
        val layoutDark = dialogView.findViewById<LinearLayout>(R.id.toogle_tema_escuro)
        val layoutSystem = dialogView.findViewById<LinearLayout>(R.id.toogle_tema_system)
        
        val switchLight = dialogView.findViewById<MaterialSwitch>(R.id.radioLight)
        val switchDark = dialogView.findViewById<MaterialSwitch>(R.id.radioDark)
        val switchSystem = dialogView.findViewById<MaterialSwitch>(R.id.radioSystem)
        
        val btnCancelar = dialogView.findViewById<Button>(R.id.btn_cancelar)

        // Inicializar os switches baseado no tema atual
        switchLight.isChecked = currentMode == AppCompatDelegate.MODE_NIGHT_NO
        switchDark.isChecked = currentMode == AppCompatDelegate.MODE_NIGHT_YES
        switchSystem.isChecked = currentMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_KingOfBozo_AlertDialog)
            .setView(dialogView)
            .create()

        fun updateTheme(mode: Int) {
            ThemeStorage.saveNightMode(requireContext(), mode)
            updateNightModeDisplay()
            dialog.dismiss()
        }

        layoutLight.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_NO) }
        switchLight.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_NO) }

        layoutDark.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_YES) }
        switchDark.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_YES) }

        layoutSystem.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        switchSystem.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateNightModeDisplay() {
        val mode = ThemeStorage.getNightMode(requireContext())
        when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                binding.iconMode.setImageResource(R.drawable.ic_light_mode)
                binding.infoMode.text = "MODO CLARO"
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                binding.iconMode.setImageResource(R.drawable.ic_dark_mode)
                binding.infoMode.text = "MODO ESCURO"
            }
            else -> {
                binding.iconMode.setImageResource(R.drawable.ic_system_mode)
                binding.infoMode.text = "MODO SISTEMA"
            }
        }
    }

    private fun updateVersionDisplay() {
        binding.textVersaoNum.text = BuildConfig.VERSION_NAME
    }

    private fun configurarCoresDaBarra() {
        val window = requireActivity().window
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.background)
        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoFundo

        val controller = androidx.core.view.WindowInsetsControllerCompat(window, binding.root)
        val isLightBackground = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_NO
        controller.isAppearanceLightStatusBars = isLightBackground
        controller.isAppearanceLightNavigationBars = isLightBackground
    }

    private fun updateThemePreview() {
        val colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, 0)
        binding.cardCor.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPrimary)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
