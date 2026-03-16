package com.wendellugalds.kingofbozo.ui.game

import android.animation.ValueAnimator
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.*
import com.wendellugalds.kingofbozo.model.CategoryType
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.ui.game.adapter.CategoryAdapter
import com.wendellugalds.kingofbozo.ui.game.adapter.PlayerMarkerAdapter

@Suppress("DEPRECATION")
class MarcadorFragment : Fragment() {

    private var _binding: FragmentMarcarJogoBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var playerMarkerAdapter: PlayerMarkerAdapter
    private var lastPlayerIndex: Int = -1
    private var lastTotalScore: Int = 0
    private var scoreAnimator: ValueAnimator? = null
    private var isFirstLoad = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMarcarJogoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupCategoryButtons()
        observeGameState()
        configurarCoresDaBarra()
        setupOnBackPressed()

        binding.buttonBack.setOnClickListener { showExitConfirmationDialog() }
        
        binding.btnJogarMaisUm.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.marcadorFragment) {
                findNavController().navigate(R.id.action_marcadorFragment_to_rankingDuranteJogoFragment)
            }
        }

        binding.buttonGerenciarJogadores.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.marcadorFragment) {
                findNavController().navigate(R.id.action_marcadorFragment_to_managePlayersFragment)
            }
        }
    }

    private fun configurarCoresDaBarra() {
        val window = requireActivity().window
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.background)
        val corDoNavegation = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.cardBackgroundColor)
        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoNavegation
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, binding.root)
        val isLightBackground = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_NO
        controller.isAppearanceLightStatusBars = isLightBackground
        controller.isAppearanceLightNavigationBars = isLightBackground
    }

    private fun setupRecyclerView() {
        playerMarkerAdapter = PlayerMarkerAdapter { index ->
            gameViewModel.setCurrentPlayer(index)
        }
        binding.recyclerViewJogadoresRodada.apply {
            adapter = playerMarkerAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            clipChildren = false
            clipToPadding = false

            val snapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(this)

            post {
                val itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 81f, resources.displayMetrics).toInt()
                val horizontalPadding = (width / 2) - (itemWidth / 2)
                setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)
                
                gameViewModel.gameState.value?.let { state ->
                    scrollToPositionCentered(state.currentPlayerIndex, isImmediate = true)
                }
            }
        }
    }

    private fun setupOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    private fun showExitConfirmationDialog() {
        val dialogBinding = DialogExitGameBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnSave.setOnClickListener {
            gameViewModel.saveCurrentGame()
            dialog.dismiss()
            findNavController().navigate(R.id.navigation_saved_games)
        }

        dialogBinding.btnExitNoSave.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.navigation_saved_games)
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun setupCategoryButtons() {
        val buttons = mapOf(
            binding.valorAz to CategoryType.AS,
            binding.valorDuque to CategoryType.DUQUE,
            binding.valorTerno to CategoryType.TERNO,
            binding.valorQuadra to CategoryType.QUADRA,
            binding.valorQuina to CategoryType.QUINA,
            binding.valorSena to CategoryType.SENA,
            binding.valorFull to CategoryType.FULL,
            binding.valorSeguida to CategoryType.SEGUIDA,
            binding.valorQuadrada to CategoryType.QUADRADA,
            binding.valorGeneral to CategoryType.GENERAL
        )

        buttons.forEach { (view, type) ->
            view.setOnClickListener { openScoringBottomSheet(type) }
        }
    }

    private fun openScoringBottomSheet(type: CategoryType) {
        val dialog = BottomSheetDialog(requireContext())
        val layoutId = when (type) {
            CategoryType.AS -> R.layout.box_az
            CategoryType.DUQUE -> R.layout.box_duque
            CategoryType.TERNO -> R.layout.box_terno
            CategoryType.QUADRA -> R.layout.box_quadra
            CategoryType.QUINA -> R.layout.box_quina
            CategoryType.SENA -> R.layout.box_sena
            CategoryType.FULL -> R.layout.box_full
            CategoryType.SEGUIDA -> R.layout.box_seguida
            CategoryType.QUADRADA -> R.layout.box_quadrada
            CategoryType.GENERAL -> R.layout.box_general
        }
        
        val sheetView = layoutInflater.inflate(layoutId, null)
        dialog.setContentView(sheetView)

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.let {
                it.setBackgroundResource(android.R.color.transparent)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        setupSheetValueClicks(sheetView, dialog, type)
        
        dialog.show()
    }

    private fun setupSheetValueClicks(view: View, dialog: BottomSheetDialog, type: CategoryType) {
        val values = listOf(R.id.valor_01, R.id.valor_02, R.id.valor_03, R.id.valor_04, R.id.valor_05, R.id.valor_boca)

        view.findViewById<View>(R.id.button_info)?.setOnClickListener { btn ->
            val infoDados = view.findViewById<View>(R.id.info_dados)
            if (infoDados != null) {

                infoDados.isVisible = !infoDados.isVisible

            }
        }

        view.findViewById<View>(R.id.btn_nulo)?.setOnClickListener {
            gameViewModel.submitScore(type, 0, isScratch = false, shouldAutoAdvance = false, isClear = true)
            dialog.dismiss()
        }

        view.findViewById<TextView>(R.id.btn_riscar)?.setOnClickListener {
            gameViewModel.submitScore(type, 0, isScratch = true, shouldAutoAdvance = true, isClear = false)
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.valor_ganhou_boca)?.setOnClickListener {
            showConfirmBocaDialog(type, dialog)
        }

        values.forEach { id ->
            view.findViewById<View>(id)?.setOnClickListener { v ->
                val clickedView = v
                val isBoca = clickedView.id == R.id.valor_boca

                val score = when {
                    isBoca && type == CategoryType.FULL -> 15
                    isBoca && type == CategoryType.SEGUIDA -> 25
                    isBoca && type == CategoryType.QUADRADA -> 35
                    isBoca && type == CategoryType.GENERAL -> 1000
                    clickedView is TextView -> clickedView.text.toString().toIntOrNull() ?: 0
                    else -> 0
                }
                
                gameViewModel.submitScore(type, score, isScratch = false, shouldAutoAdvance = true, isClear = false)
                dialog.dismiss()
            }
        }
        
        view.findViewById<View>(R.id.button_cancel)?.setOnClickListener { dialog.dismiss() }
    }

    private fun showConfirmBocaDialog(type: CategoryType, parentDialog: BottomSheetDialog) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_boca, null)
        val alertDialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_confirm).setOnClickListener {
            val score = when (type) {
                CategoryType.FULL -> 15
                CategoryType.SEGUIDA -> 25
                CategoryType.QUADRADA -> 35
                CategoryType.GENERAL -> 1000
                else -> 0
            }
            gameViewModel.submitScore(type, score, isScratch = false, shouldAutoAdvance = true, isClear = false)
            
            alertDialog.dismiss()
            parentDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun animateScoreChange(start: Int, end: Int) {
        scoreAnimator?.cancel()
        scoreAnimator = ValueAnimator.ofInt(start, end).apply {
            duration = 600
            addUpdateListener { valueAnimator ->
                _binding?.let {
                    it.pontosRodada.text = valueAnimator.animatedValue.toString()
                }
            }
            start()
        }
    }

    private fun observeGameState() {
        gameViewModel.gameState.observe(viewLifecycleOwner) { state ->
            state?.let {
                val currentPlayer = it.playersState[it.currentPlayerIndex]
                
                if (lastPlayerIndex == -1) {
                    lastPlayerIndex = it.currentPlayerIndex
                    lastTotalScore = currentPlayer.totalScore
                    binding.pontosRodada.text = lastTotalScore.toString()
                } else if (it.currentPlayerIndex != lastPlayerIndex) {
                    lastPlayerIndex = it.currentPlayerIndex
                    
                    scoreAnimator?.cancel()
                    animateScoreChange(0, currentPlayer.totalScore)
                    lastTotalScore = currentPlayer.totalScore
                    animatePlayerSwitch()
                } else if (currentPlayer.totalScore != lastTotalScore) {
                    scoreAnimator?.cancel()
                    binding.pontosRodada.text = currentPlayer.totalScore.toString()
                    lastTotalScore = currentPlayer.totalScore
                }

                val players = it.playersState
                val maxScore = players.maxOf { p -> p.totalScore }
                val minScore = players.minOf { p -> p.totalScore }
                
                binding.textPlayerName.text = currentPlayer.playerName
                
                val isWinner = currentPlayer.totalScore == maxScore && maxScore > 0
                binding.iconKing.isVisible = isWinner
                
                if (isWinner) {
                    val floatingAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.floating)
                    binding.iconKing.startAnimation(floatingAnim)
                } else {
                    binding.iconKing.clearAnimation()
                }

                if (maxScore == 0) {
                    binding.textStatus.text = ""
                    binding.textStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                } else {
                    val tiedWithOthers = players.filter { it.playerName != currentPlayer.playerName && it.totalScore == currentPlayer.totalScore }
                    
                    if (tiedWithOthers.isNotEmpty()) {
                        val othersNames = tiedWithOthers.joinToString(", ") { it.playerName.substringBefore(" ") }
                        binding.textStatus.text = "Empatado com $othersNames"
                        binding.textStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bug_tie, 0, 0, 0)
                    } else {
                        when (currentPlayer.totalScore) {
                            maxScore -> {
                                binding.textStatus.text = "Ganhando"
                                binding.textStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_crown, 0, 0, 0)
                            }
                            minScore -> {
                                binding.textStatus.text = "Perdendo"
                                binding.textStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_perdendo, 0, 0, 0)
                            }
                            else -> {
                                binding.textStatus.text = " "
                                binding.textStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                            }
                        }
                    }
                }
                
                updatePlayerAvatar(currentPlayer)

                if (isFirstLoad) {
                    isFirstLoad = false
                    playerMarkerAdapter.updateState(it.currentPlayerIndex, it.playersState)
                    scrollToPositionCentered(it.currentPlayerIndex, isImmediate = true)
                } else {
                    scrollToPositionCentered(it.currentPlayerIndex, isImmediate = false) {
                        playerMarkerAdapter.updateState(it.currentPlayerIndex, it.playersState)
                    }
                }

                binding.totalRodadas.text = "RODADA ${it.currentRound.toString().padStart(2, '0')}"
                updateCategoryButtonsForPlayer(currentPlayer)
            }
        }

        gameViewModel.navigateToRanking.observe(viewLifecycleOwner) { navigate ->
            if (navigate) {
                if (findNavController().currentDestination?.id == R.id.marcadorFragment) {
                    findNavController().navigate(R.id.action_marcadorFragment_to_rankingFragment)
                }
                gameViewModel.onRankingNavigated()
            }
        }
    }

    private fun animatePlayerSwitch() {
        binding.formAddPlayer.clearAnimation()
        binding.formAddPlayer.translationY = 100f
        binding.formAddPlayer.alpha = 0f
        binding.formAddPlayer.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.avatarContainer.clearAnimation()
        binding.avatarContainer.scaleX = 0.7f
        binding.avatarContainer.scaleY = 0.7f
        binding.avatarContainer.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(500)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
    }

    private fun scrollToPositionCentered(position: Int, isImmediate: Boolean, onComplete: (() -> Unit)? = null) {
        val recyclerView = binding.recyclerViewJogadoresRodada
        recyclerView.post {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val offset = recyclerView.paddingStart

            if (isImmediate) {
                layoutManager.scrollToPositionWithOffset(position, offset)
                onComplete?.invoke()
            } else {
                val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                    override fun getHorizontalSnapPreference(): Int = SNAP_TO_START
                    override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                        return 450f / displayMetrics.densityDpi
                    }
                    override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
                        val dx = calculateDxToMakeVisible(targetView, horizontalSnapPreference)
                        val dy = calculateDyToMakeVisible(targetView, verticalSnapPreference)
                        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toInt()
                        val time = Math.max(1200, calculateTimeForDeceleration(distance))
                        if (time > 0) {
                            action.update(-dx, -dy, time, DecelerateInterpolator(3.0f))
                        }
                    }
                    override fun onStop() {
                        super.onStop()
                        recyclerView.post { onComplete?.invoke() }
                    }
                }
                smoothScroller.targetPosition = position
                layoutManager.startSmoothScroll(smoothScroller)
            }
        }
    }

    private fun updatePlayerAvatar(player: PlayerState) {
        if (!player.playerImage.isNullOrEmpty()) {
            binding.imagePlayerAvatarDetail.visibility = View.VISIBLE
            binding.imagePlayerAvatarDetail.load(player.playerImage)
            binding.siglaNome.visibility = View.GONE
        } else {
            binding.imagePlayerAvatarDetail.visibility = View.GONE
            binding.siglaNome.visibility = View.VISIBLE
            binding.siglaNome.text = player.playerName.take(2).uppercase()
        }
    }

    private fun updateCategoryButtonsForPlayer(player: PlayerState) {
        val categoryMap = mapOf(
            CategoryType.AS to binding.valorAz,
            CategoryType.DUQUE to binding.valorDuque,
            CategoryType.TERNO to binding.valorTerno,
            CategoryType.QUADRA to binding.valorQuadra,
            CategoryType.QUINA to binding.valorQuina,
            CategoryType.SENA to binding.valorSena,
            CategoryType.FULL to binding.valorFull,
            CategoryType.SEGUIDA to binding.valorSeguida,
            CategoryType.QUADRADA to binding.valorQuadrada,
            CategoryType.GENERAL to binding.valorGeneral
        )
        val namesMap = mapOf(
            CategoryType.AS to "Áz",
            CategoryType.DUQUE to "Duque",
            CategoryType.TERNO to "Terno",
            CategoryType.QUADRA to "Quadra",
            CategoryType.QUINA to "Quina",
            CategoryType.SENA to "Sena",
            CategoryType.FULL to "Full",
            CategoryType.SEGUIDA to "Seguida",
            CategoryType.QUADRADA to "Quadrada",
            CategoryType.GENERAL to "General"
        )
        categoryMap.forEach { (type, btn) ->
            val scoreEntry = player.scores[type]
            CategoryAdapter.applyCategoryStyle(
                textView = btn,
                root = btn,
                categoryName = namesMap[type] ?: "",
                score = scoreEntry?.value,
                isScored = scoreEntry?.isScored ?: false,
                isScratch = scoreEntry?.isScratch ?: false,
                isBoca = scoreEntry?.isBoca ?: false,
                context = requireContext()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scoreAnimator?.cancel()
        _binding = null
        lastPlayerIndex = -1
        lastTotalScore = 0
    }
}
