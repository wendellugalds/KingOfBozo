package com.wendellugalds.kingofbozo.ui.game

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.databinding.BottomSheetRankingGeralBinding
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.ui.game.adapter.RankingGeralAdapter

class RankingGeralBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRankingGeralBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var rankingGeralAdapter: RankingGeralAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetRankingGeralBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            // Pega a cor EXATA da Activity (colorPrimary do tema)[cite: 7]
            val typedValue = android.util.TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            val corDoPainel = typedValue.data

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                // Converte os 55dp para pixels reais da tela[cite: 7]
                val radius55 = 55f * resources.displayMetrics.density

                // Cria o fundo dinâmico com cantos arredondados em cima e retos embaixo[cite: 7]
                val shapeDrawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(corDoPainel)
                    cornerRadii = floatArrayOf(radius55, radius55, radius55, radius55, 0f, 0f, 0f, 0f)
                }
                it.background = shapeDrawable

                // Monitora a altura da tela para zerar o arredondamento de cima se o teclado subir[cite: 7]
                it.viewTreeObserver.addOnGlobalLayoutListener {
                    val r = android.graphics.Rect()
                    it.getWindowVisibleDisplayFrame(r)
                    val screenHeight = it.rootView.height
                    val keypadHeight = screenHeight - r.bottom
                    val isKeyboardVisible = keypadHeight > screenHeight * 0.15

                    if (isKeyboardVisible) {
                        shapeDrawable.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                    } else {
                        shapeDrawable.cornerRadii = floatArrayOf(radius55, radius55, radius55, radius55, 0f, 0f, 0f, 0f)
                    }
                }
            }

            bottomSheetDialog.window?.let { window ->
                // Pinta a barra de navegação inferior com a cor correta do tema[cite: 7]
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                window.navigationBarColor = corDoPainel

                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

                // Desliga a sombra escura forçada da One UI (Samsung)[cite: 7]
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeGameState()
    }

    private fun setupRecyclerView() {
        rankingGeralAdapter = RankingGeralAdapter()
        binding.recyclerViewListaJogadoresRankingGeralJogoAtual.apply {
            adapter = rankingGeralAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeGameState() {
        gameViewModel.gameState.observe(viewLifecycleOwner) { state ->
            state?.let {
                binding.totalRodadas.text = "${it.currentRound} Rodadas"

                val sortedGeral = it.playersState.sortedWith(
                    compareByDescending<PlayerState> { p -> p.sessionWins }
                        .thenByDescending { p -> p.sessionTotalPoints }
                ).filter { it.sessionWins >= 1 }

                rankingGeralAdapter.submitList(sortedGeral)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}