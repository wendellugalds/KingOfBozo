package com.wendellugalds.kingofbozo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentHomeBinding
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 0..5 -> "Ficou sem sono? Bora jogar então!"  // 00:00 às 05:59
            in 6..11 -> "Bom dia!"       // 06:00 às 11:59
            in 12..17 -> "Tarde!"    // 12:00 às 17:59
            else -> "Olá boa noite!"         // 18:00 às 23:59
        }

        binding.textGreeting.text = greeting
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- LÓGICA DO BOTÃO MARCADOR ADICIONADA ---
        binding.buttonMarcador.setOnClickListener {
            // Usa a ação global para navegar para a tela de seleção de jogadores
            findNavController().navigate(R.id.action_global_playerSelectionFragment)
        }

        // 1. Atualiza a saudação
        updateGreeting()

        configurarCoresDaBarra()
    }

    private fun configurarCoresDaBarra() {
        val window = requireActivity().window

        // Corrected reference to R.attr.background
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.background)

        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoFundo

        val controller = androidx.core.view.WindowInsetsControllerCompat(window, binding.root)
        val isLightBackground = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_NO
        controller.isAppearanceLightStatusBars = isLightBackground
        controller.isAppearanceLightNavigationBars = isLightBackground
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
