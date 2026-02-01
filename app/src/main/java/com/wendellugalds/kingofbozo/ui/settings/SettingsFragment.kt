package com.wendellugalds.kingofbozo.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.BuildConfig
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

        binding.cardCor.setOnClickListener {
            val bottomSheet = ThemeSelectionBottomSheet { themeResId ->
                ThemeStorage.saveTheme(requireContext(), themeResId)
                requireActivity().recreate()
            }
            bottomSheet.show(parentFragmentManager, "ThemeSelection")
        }

    }

    private fun updateVersionDisplay() {
        // Usa a versionName do BuildConfig gerada pelo Gradle
        binding.textVersaoNum.text = BuildConfig.VERSION_NAME
    }
    private fun configurarCoresDaBarra() {
        // Pega a janela da Activity (a tela inteira)
        val window = requireActivity().window

        // Pega a cor "Surface" do seu tema atual (a mesma usada em BottomSheets e fundos)
        // Se quiser outra cor, troque R.attr.colorSurface por R.attr.colorPrimary, etc.
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.background)

        // Aplica a cor na Barra de Status (Topo)
        window.statusBarColor = corDoFundo

        // Aplica a cor na Barra de Navegação (Rodapé)
        window.navigationBarColor = corDoFundo

        // (Opcional) Ajusta a cor dos ícones (bateria, wifi, botões de voltar)
        // Use 'true' se o fundo for CLARO (ícones pretos)
        // Use 'false' se o fundo for ESCURO (ícones brancos)
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, binding.root)
        val isLightBackground = false // <--- Mude para true se seu tema for claro
        controller.isAppearanceLightStatusBars = isLightBackground
        controller.isAppearanceLightNavigationBars = isLightBackground
    }
    private fun updateThemePreview() {
        val colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, 0)
        binding.cardCor.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPrimary)
        
//        // Atualiza o texto baseado na cor (opcional, mas bom para UX)
//        val currentThemeId = ThemeStorage.getTheme(requireContext())
//        binding.textCorDesc.text = if (currentThemeId == com.wendellugalds.kingofbozo.R.style.Base_Theme_KingOfBozo_verde_neon) {
//            "Verde Neon"
//        } else {
//            "Roxo Elétrico"
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
