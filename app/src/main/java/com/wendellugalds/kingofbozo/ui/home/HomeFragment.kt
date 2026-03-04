package com.wendellugalds.kingofbozo.ui.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentHomeBinding
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonMarcador.setOnClickListener {
            findNavController().navigate(R.id.action_global_playerSelectionFragment)
        }

        updateGreeting()
        configurarCoresDaBarra()
        iniciarAnimacoesLoop()
    }

    private fun iniciarAnimacoesLoop() {
        // Animação da Coroa Flutuando Suavemente
        ObjectAnimator.ofFloat(binding.crown, "translationY", 0f, -15f).apply {
            duration = 1500L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 0..5 -> "FICOU SEM SONO?\nBORA JOGAR ENTÃO!"
            in 6..11 -> "BOM DIA!"
            in 12..17 -> "TARDE!"
            else -> "OLÁ BOA NOITE!"
        }

        binding.textGreeting.text = greeting
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
