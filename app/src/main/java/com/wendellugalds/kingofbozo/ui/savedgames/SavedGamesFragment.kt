package com.wendellugalds.kingofbozo.ui.savedgames

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.databinding.FragmentSavedGamesBinding

class SavedGamesFragment : Fragment() {

    private var _binding: FragmentSavedGamesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedGamesBinding.inflate(inflater, container, false)
        return binding.root
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}