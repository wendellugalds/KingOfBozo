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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.ui.game.GameViewModel
import com.wendellugalds.kingofbozo.ui.game.GameViewModelFactory
import com.wendellugalds.kingofbozo.util.PremiumManager
import com.wendellugalds.kingofbozo.util.SystemBarUtil
import com.wendellugalds.kingofbozo.util.AnimationUtil
import com.wendellugalds.kingofbozo.ui.PremiumBottomSheet
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentHomeBinding
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

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
            viewLifecycleOwner.lifecycleScope.launch {
                val count = gameViewModel.getSavedGamesCount()
                if (!PremiumManager.isUserPremium(requireContext()) && count >= PremiumManager.MAX_FREE_SAVED_GAMES) {
                    val premiumSheet = PremiumBottomSheet()
                    premiumSheet.show(parentFragmentManager, "PremiumBottomSheet")
                } else {
                    AnimationUtil.applyCollapseAnimation(binding.buttonMarcador) {
                        findNavController().navigate(R.id.action_global_playerSelectionFragment)
                    }
                }
            }
        }

        binding.buttonPremiumHome.setOnClickListener {
            val premiumSheet = PremiumBottomSheet()
            premiumSheet.show(parentFragmentManager, "PremiumBottomSheet")
        }

        updateGreeting()
        configurarCoresDaBarra()
        iniciarAnimacoesLoop()
        
        binding.buttonMarcador.post {
            AnimationUtil.applyExpansionAnimation(binding.buttonMarcador)
        }
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
            in 0..5 -> getString(R.string.ficou_sem_sono)
            in 6..11 -> getString(R.string.bom_dia)
            in 12..17 -> getString(R.string.tarde)
            else -> getString(R.string.ola_boa_noite)
        }

        binding.textGreeting.text = greeting
    }

    private fun configurarCoresDaBarra() {
        SystemBarUtil.applySystemBarColors(requireActivity().window, binding.root, statusBarAttr = R.attr.customBackground, navBarAttr = R.attr.cardBackgroundColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
