package com.wendellugalds.kingofbozo.ui.game

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.*
import com.wendellugalds.kingofbozo.model.CategoryType
import com.wendellugalds.kingofbozo.model.GameStatus
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.model.TieBreakerState
import com.wendellugalds.kingofbozo.ui.PremiumBottomSheet
import com.wendellugalds.kingofbozo.ui.game.adapter.CategoryAdapter
import com.wendellugalds.kingofbozo.ui.game.adapter.PlayerMarkerAdapter
import com.wendellugalds.kingofbozo.ui.game.adapter.TieBreakerAdapter
import com.wendellugalds.kingofbozo.util.SystemBarUtil
import com.wendellugalds.kingofbozo.util.AnimationUtil
import com.wendellugalds.kingofbozo.util.AdManager

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
    private var tieBreakerDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMarcarJogoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupCategoryButtons()
        observeGameState()
        observeTieBreakerState()
        configurarCoresDaBarra()
        setupNavigationModeMargins()
        setupOnBackPressed()

        binding.buttonBack.setOnClickListener { showExitConfirmationDialog() }
        
        binding.btnRankingDuranteJogo.setOnClickListener {
            findNavController().navigate(R.id.action_marcadorFragment_to_rankingDuranteJogoFragment)
        }

        binding.buttonGerenciarJogadores.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.marcadorFragment) {
                findNavController().navigate(R.id.action_marcadorFragment_to_managePlayersFragment)
            }
        }
    }

    private fun setupNavigationModeMargins() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val isGestureNavigation = navBarInset.bottom <= TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics
            )

            val recyclerBottomMargin = if (isGestureNavigation) {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25f, resources.displayMetrics).toInt()
            } else {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()
            }

            val gridBottomMargin = if (isGestureNavigation) {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics).toInt()
            } else {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 65f, resources.displayMetrics).toInt()
            }

            binding.recyclerViewJogadoresRodada.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = recyclerBottomMargin
            }

            binding.gridLayoutCategories.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = gridBottomMargin
            }

            insets
        }
    }

    private fun observeTieBreakerState() {
        gameViewModel.tieBreakerUiState.observe(viewLifecycleOwner) { state ->
            handleTieBreakerState(state)
        }
    }

    private fun handleTieBreakerState(state: TieBreakerState) {
        tieBreakerDialog?.dismiss()
        tieBreakerDialog = null

        when (state) {
            is TieBreakerState.ShowTiedPlayers -> showTiedPlayersDialog(state.tiedPlayers)
            is TieBreakerState.ShowScoreInput -> showScoreInputDialog(state.player)
            is TieBreakerState.ConfirmExit -> showExitConfirmDialog()
            TieBreakerState.Idle -> { }
        }
    }

    private fun showTiedPlayersDialog(tiedPlayers: List<PlayerState>) {
        val dialogBinding = DialogTieBreakerListBinding.inflate(layoutInflater)
        
        fun updateDialogState() {
            val tempValues = gameViewModel.tempTieBreakerValues.value.orEmpty()
            val adapter = TieBreakerAdapter(tempValues) { player ->
                gameViewModel.onPlayerSelectedForTieBreak(player)
            }
            
            dialogBinding.recyclerTiedPlayers.layoutManager = LinearLayoutManager(requireContext())
            dialogBinding.recyclerTiedPlayers.adapter = adapter
            adapter.submitList(tiedPlayers)

            // Lógica do botão confirmar: só aparece se houver vencedor isolado
            val finalScores = tiedPlayers.map { it.totalScore + (tempValues[it.playerId] ?: 0) }
            val maxScore = finalScores.maxOrNull() ?: 0
            val isTieResolved = finalScores.count { it == maxScore } == 1
            
            dialogBinding.btnConfirmTie.isVisible = isTieResolved
        }

        updateDialogState()

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        // Sincroniza o botão "Voltar" do sistema com o botão de fechar "X"
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                gameViewModel.dismissTieBreaker()
                true
            } else false
        }

        dialogBinding.btnClose.setOnClickListener {
            gameViewModel.dismissTieBreaker()
        }

        dialogBinding.btnConfirmTie.setOnClickListener {
            gameViewModel.resolveTie()
        }

        tieBreakerDialog = dialog
        dialog.show()
    }

    private fun showScoreInputDialog(player: PlayerState) {
        val dialogBinding = DialogTieBreakerInputBinding.inflate(layoutInflater)
        dialogBinding.textPlayerName.text = player.playerName
        dialogBinding.textCurrentScore.text = "${player.totalScore} pontos"

        // Configura foto ou iniciais do jogador
        if (!player.playerImage.isNullOrEmpty()) {
            dialogBinding.imagePlayerAvatar.load(Uri.parse(player.playerImage)) {
                crossfade(true)
                placeholder(R.drawable.ic_person)
                transformations(CircleCropTransformation())
            }
            dialogBinding.imagePlayerAvatar.visibility = View.VISIBLE
            dialogBinding.siglaNome.visibility = View.GONE
        } else {
            dialogBinding.imagePlayerAvatar.visibility = View.GONE
            dialogBinding.siglaNome.visibility = View.VISIBLE
            val name = player.playerName.trim()
            val words = name.split(" ").filter { it.isNotBlank() }
            dialogBinding.siglaNome.text = if (words.size > 1) {
                "${words.first().first()}${words.last().first()}"
            } else if (words.isNotEmpty()) {
                words.first().take(2).uppercase()
            } else {
                "--"
            }
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        // Sincroniza o botão "Voltar" do sistema com o botão de fechar "X"
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                gameViewModel.cancelExitConfirm()
                true
            } else false
        }

        dialogBinding.btnDie1.setOnClickListener { gameViewModel.setTieBreakerValue(player, 1) }
        dialogBinding.btnDie2.setOnClickListener { gameViewModel.setTieBreakerValue(player, 2) }
        dialogBinding.btnDie3.setOnClickListener { gameViewModel.setTieBreakerValue(player, 3) }
        dialogBinding.btnDie4.setOnClickListener { gameViewModel.setTieBreakerValue(player, 4) }
        dialogBinding.btnDie5.setOnClickListener { gameViewModel.setTieBreakerValue(player, 5) }
        dialogBinding.btnDie6.setOnClickListener { gameViewModel.setTieBreakerValue(player, 6) }

        dialogBinding.btnClearTie.setOnClickListener {
            gameViewModel.clearTieBreakerValue(player)
        }

        dialogBinding.btnCloseInput.setOnClickListener {
            gameViewModel.cancelExitConfirm() // Volta para a lista
        }

        tieBreakerDialog = dialog
        dialog.show()
    }

    private fun showExitConfirmDialog() {
        val dialogBinding = DialogConfirmExitTieBreakBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        // Sincroniza o botão "Voltar" do sistema com o botão "Cancelar/Voltar"
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                gameViewModel.cancelExitConfirm()
                true
            } else false
        }

        dialogBinding.btnConfirm.setOnClickListener {
            dialog.dismiss()
            gameViewModel.saveCurrentGame(requireContext())
            if (gameViewModel.showPremiumLimitEvent.value == null) {
                findNavController().navigate(R.id.navigation_saved_games)
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            gameViewModel.cancelExitConfirm() // Volta para a lista de empatados
        }

        tieBreakerDialog = dialog
        dialog.show()
    }

    private fun configurarCoresDaBarra() {
        SystemBarUtil.applySystemBarColors(
            requireActivity().window,
            binding.root,
            statusBarAttr = R.attr.cardBackgroundColor,
            navBarAttr = R.attr.colorPrimary
        )
    }

    private fun setupRecyclerView() {
        playerMarkerAdapter = PlayerMarkerAdapter { index ->
            gameViewModel.setCurrentPlayer(index)
        }
        binding.recyclerViewJogadoresRodada.apply {
            adapter = playerMarkerAdapter
            val lm = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            layoutManager = lm
            clipChildren = false
            clipToPadding = false

            val snapHelper = object : PagerSnapHelper() {
                override fun findTargetSnapPosition(
                    layoutManager: RecyclerView.LayoutManager,
                    velocityX: Int,
                    velocityY: Int
                ): Int {
                    val centerView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
                    val currentPosition = layoutManager.getPosition(centerView)
                    if (currentPosition == RecyclerView.NO_POSITION) return RecyclerView.NO_POSITION

                    return when {
                        velocityX > 300 -> (currentPosition + 1).coerceAtMost(layoutManager.itemCount - 1)
                        velocityX < -300 -> (currentPosition - 1).coerceAtLeast(0)
                        else -> currentPosition
                    }
                }
            }
            snapHelper.attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateItemScalesAndAlpha(recyclerView)
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val centerView = snapHelper.findSnapView(lm)
                        if (centerView != null) {
                            val snappedPosition = lm.getPosition(centerView)
                            val currentActiveIndex = gameViewModel.gameState.value?.currentPlayerIndex ?: -1
                            if (snappedPosition != RecyclerView.NO_POSITION && snappedPosition != currentActiveIndex) {
                                gameViewModel.setCurrentPlayer(snappedPosition)
                            }
                        }
                    }
                }
            })

            post {
                val firstView = lm.findViewByPosition(0)
                val itemWidth = if (firstView != null && firstView.width > 0) {
                    firstView.width
                } else {
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 89f, resources.displayMetrics).toInt()
                }
                val horizontalPadding = (width / 2) - (itemWidth / 3)
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

        val navOptions = androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.navigation_home, false)
            .build()

        dialogBinding.btnSave.setOnClickListener {
            gameViewModel.saveCurrentGame(requireContext())
            dialog.dismiss()
            if (gameViewModel.showPremiumLimitEvent.value == null) {
                findNavController().navigate(R.id.navigation_saved_games, null, navOptions)
            }
        }

        dialogBinding.btnExitNoSave.setOnClickListener {
            gameViewModel.discardUnsavedChanges()
            dialog.dismiss()
            findNavController().navigate(R.id.navigation_saved_games, null, navOptions)
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

            // 1. Pega a cor EXATA do tema (R.attr.customBackground)
            val typedValue = android.util.TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.customBackground, typedValue, true)
            val corDoPainel = typedValue.data

            // 2. Configurações da View (BottomSheet) - Fundo sólido com cantos arredondados
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                val radius55 = 55f * resources.displayMetrics.density
                val shapeDrawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(corDoPainel)
                    cornerRadii = floatArrayOf(radius55, radius55, radius55, radius55, 0f, 0f, 0f, 0f)
                }
                sheet.background = shapeDrawable
            }

            // 3. Configurações da Janela (Window) - Cor da barra de navegação idêntica
            bottomSheetDialog.window?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                window.navigationBarColor = corDoPainel

                val controller = androidx.core.view.WindowInsetsControllerCompat(window, sheetView)
                val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                controller.isAppearanceLightNavigationBars = !isNightMode

                // Desliga a sombra escura forçada da One UI (Samsung)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
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
        
//        view.findViewById<View>(R.id.button_cancel)?.setOnClickListener { dialog.dismiss() }
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
                val winnersCount = players.count { p -> p.totalScore == maxScore && p.totalScore > 0 }
                
                binding.textPlayerName.text = currentPlayer.playerName
                
                val isWinner = currentPlayer.totalScore == maxScore && winnersCount == 1 && maxScore > 0
                val playersBetter = players.count { p -> p.totalScore > currentPlayer.totalScore }
                val playerRank = playersBetter + 1

                // 1º lugar isolado exibe a Coroa e esconde o ícone de posição no ranking (icon_position_raking)
                binding.iconKing.isVisible = isWinner
                binding.iconPositionRaking.isVisible = !isWinner
                
                if (isWinner) {
                    val floatingAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.floating)
                    binding.iconKing.startAnimation(floatingAnim)
                } else {
                    binding.iconKing.clearAnimation()

                    val positionIcon = when (playerRank) {
                        1 -> R.drawable.ic_1
                        2 -> R.drawable.ic_2
                        3 -> R.drawable.ic_3
                        4 -> R.drawable.ic_4
                        5 -> R.drawable.ic_5
                        6 -> R.drawable.ic_6
                        7 -> R.drawable.ic_7
                        8 -> R.drawable.ic_8
                        9 -> R.drawable.ic_9
                        else -> R.drawable.ic_0
                    }
                    binding.iconPositionRaking.setImageResource(positionIcon)

                    val rankColor = when (playerRank) {
                        2 -> ContextCompat.getColor(requireContext(), R.color.silver)
                        3 -> ContextCompat.getColor(requireContext(), R.color.bronze)
                        else -> MaterialColors.getColor(requireContext(), R.attr.colorPrimary, 0)
                    }
                    ImageViewCompat.setImageTintList(binding.iconPositionRaking, ColorStateList.valueOf(rankColor))
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
                        val distinctScores = players.map { it.totalScore }.distinct().sortedDescending()
                        val scoreTierIndex = distinctScores.indexOf(currentPlayer.totalScore)
                        
                        if (scoreTierIndex == 0) {
                            binding.textStatus.text = "Ganhando"
                            binding.textStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_crown, 0, 0, 0)
                        } else {
                            val scoreAbove = distinctScores[scoreTierIndex - 1]
                            val diff = scoreAbove - currentPlayer.totalScore
                            val playersBetterThanAbove = players.count { it.totalScore > scoreAbove }
                            val rankAbove = playersBetterThanAbove + 1
                            binding.textStatus.text = "Perdendo de \n$diff pontos do ${rankAbove}º lugar"
                            binding.textStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_perdendo, 0, 0, 0)
                        }
                    }
                }
                
                updatePlayerAvatar(currentPlayer)

                if (isFirstLoad) {
                    isFirstLoad = false
                    playerMarkerAdapter.updateState(it.currentPlayerIndex, it.playersState)
                    scrollToPositionCentered(it.currentPlayerIndex, isImmediate = true)
                } else {
                    playerMarkerAdapter.updateState(it.currentPlayerIndex, it.playersState)
                    scrollToPositionCentered(it.currentPlayerIndex, isImmediate = false)
                }

                binding.totalRodadas.text = "RODADA ${it.currentRound.toString().padStart(2, '0')}"
                updateCategoryButtonsForPlayer(currentPlayer)

                // Regra: O botão Ranking só aparece se TODOS os jogadores ativos já tiverem jogado ao menos uma vez (pontuado ou riscado)
                val haveAllPlayersPlayed = players.all { player ->
                    player.scores.values.any { entry -> entry != null }
                }
                binding.btnRankingDuranteJogo.isVisible = haveAllPlayersPlayed
            }
        }

        gameViewModel.navigateToRanking.observe(viewLifecycleOwner) { navigate ->
            if (navigate) {
                view?.post {
                    if (isAdded && findNavController().currentDestination?.id == R.id.marcadorFragment) {
                        findNavController().navigate(R.id.action_marcadorFragment_to_rankingFragment)
                    }
                    gameViewModel.onRankingNavigated()
                }
            }
        }

        gameViewModel.showPremiumLimitEvent.observe(viewLifecycleOwner) { limitType ->
            if (limitType == 1) {
                Toast.makeText(requireContext(), getString(R.string.premium_limit_games_msg), Toast.LENGTH_LONG).show()
                val premiumSheet = PremiumBottomSheet()
                premiumSheet.show(parentFragmentManager, "PremiumBottomSheet")
                gameViewModel.resetPremiumLimitEvent()
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

    private fun updateItemScalesAndAlpha(recyclerView: RecyclerView) {
        val recyclerCenter = recyclerView.width / 2f
        if (recyclerCenter <= 0f) return

        val itemWidth = if (recyclerView.childCount > 0) {
            recyclerView.getChildAt(0).width.toFloat()
        } else {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 89f, resources.displayMetrics)
        }

        // Distância para atingir a menor escala (1.5x a largura do item)
        val maxScaleDistance = itemWidth * 1.9f

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val childCenter = (child.left + child.right) / 2f
            val distanceFromCenter = Math.abs(recyclerCenter - childCenter)

            val fraction = (distanceFromCenter / maxScaleDistance).coerceIn(0f, 1f)

            // Escala: 1.05f no centro -> 0.55f no vizinho direto (~metade da escala) -> 0.35f nos mais distantes
            val scale = 1.05f - (fraction * 0.70f)
            child.scaleX = scale
            child.scaleY = scale

            // Opacidade: 1.0f no centro -> 0.5f no vizinho -> 0.25f nos mais distantes
            val alpha = 1.0f - (fraction * 0.75f)
            child.alpha = alpha.coerceAtLeast(0.2f)
        }
    }

    private fun scrollToPositionCentered(position: Int, isImmediate: Boolean = false, onComplete: (() -> Unit)? = null) {
        val recyclerView = binding.recyclerViewJogadoresRodada
        recyclerView.post {
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return@post
            val targetView = layoutManager.findViewByPosition(position)

            if (targetView != null) {
                val recyclerCenter = recyclerView.width / 2
                val viewCenter = targetView.left + (targetView.width / 2)
                val dx = viewCenter - recyclerCenter

                if (isImmediate) {
                    if (Math.abs(dx) > 0) {
                        recyclerView.scrollBy(dx, 0)
                    }
                    updateItemScalesAndAlpha(recyclerView)
                    onComplete?.invoke()
                } else {
                    if (Math.abs(dx) > 2) {
                        val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                            override fun getHorizontalSnapPreference(): Int = SNAP_TO_START
                            override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                                return 150f / displayMetrics.densityDpi
                            }
                            override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
                                val dxToCenter = (targetView.left + targetView.width / 2) - (recyclerView.width / 2)
                                action.update(dxToCenter, 0, 300, DecelerateInterpolator(1.8f))
                            }
                            override fun onStop() {
                                super.onStop()
                                onComplete?.invoke()
                            }
                        }
                        smoothScroller.targetPosition = position
                        layoutManager.startSmoothScroll(smoothScroller)
                    } else {
                        updateItemScalesAndAlpha(recyclerView)
                        onComplete?.invoke()
                    }
                }
            } else {
                val itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 89f, resources.displayMetrics).toInt()
                val offset = (recyclerView.width / 2) - (itemWidth / 2)
                if (isImmediate) {
                    layoutManager.scrollToPositionWithOffset(position, offset)
                    recyclerView.post { updateItemScalesAndAlpha(recyclerView) }
                    onComplete?.invoke()
                } else {
                    val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                        override fun getHorizontalSnapPreference(): Int = SNAP_TO_START
                        override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                            return 150f / displayMetrics.densityDpi
                        }
                        override fun onStop() {
                            super.onStop()
                            onComplete?.invoke()
                        }
                    }
                    smoothScroller.targetPosition = position
                    layoutManager.startSmoothScroll(smoothScroller)
                }
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
