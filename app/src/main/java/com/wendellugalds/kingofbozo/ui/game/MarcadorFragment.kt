package com.wendellugalds.kingofbozo.ui.game

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
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
import com.wendellugalds.kingofbozo.ui.game.adapter.PlayerMarkerAdapter

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
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)
        val corDoNavegation = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.cardBackgroundColor)
        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoNavegation
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, binding.root)
        val isLightBackground = false 
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
                } else if (currentPlayer.totalScore != lastTotalScore) {
                    scoreAnimator?.cancel()
                    binding.pontosRodada.text = currentPlayer.totalScore.toString()
                    lastTotalScore = currentPlayer.totalScore
                }

                val players = it.playersState
                val maxScore = players.maxOfOrNull { p -> p.totalScore } ?: 0
                val winnersCount = players.count { p -> p.totalScore == maxScore && maxScore > 0 }
                
                binding.textPlayerName.text = currentPlayer.playerName
                
                val tiedPlayers = players.filter { it.totalScore == currentPlayer.totalScore && it.totalScore > 0 }
                val isWinning = currentPlayer.totalScore == maxScore && maxScore > 0

                if (isWinning) {
                    if (winnersCount > 1) {
                        val others = tiedPlayers.filter { it.playerName != currentPlayer.playerName }
                        val othersNames = others.joinToString(", ") { it.playerName.substringBefore(" ") }
                        binding.textStatus.text = "Empatado com $othersNames"
                        binding.textStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bug_tie, 0, 0, 0)
                    } else {
                        binding.textStatus.text = "Ganhando"
                        binding.textStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_crown, 0, 0, 0)
                    }
                } else {
                    binding.textStatus.text = "Perdendo"
                    binding.textStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
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

    private fun scrollToPositionCentered(position: Int, isImmediate: Boolean, onComplete: (() -> Unit)? = null) {
        val recyclerView = binding.recyclerViewJogadoresRodada
        recyclerView.post {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val screenWidth = recyclerView.width
            val itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 81f, resources.displayMetrics).toInt()
            val offset = (screenWidth / 2) - (itemWidth / 2)

            if (isImmediate) {
                layoutManager.scrollToPositionWithOffset(position, offset)
                onComplete?.invoke()
            } else {
                val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                    override fun getHorizontalSnapPreference(): Int = SNAP_TO_START

                    override fun calculateDxToMakeVisible(view: View, snapPreference: Int): Int {
                        val dx = super.calculateDxToMakeVisible(view, snapPreference)
                        return dx + (screenWidth / 2) - (view.width / 2)
                    }

                    override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                        return 80f / displayMetrics.densityDpi
                    }

                    override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
                        val dx = calculateDxToMakeVisible(targetView, horizontalSnapPreference)
                        val dy = calculateDyToMakeVisible(targetView, verticalSnapPreference)
                        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toInt()
                        val time = calculateTimeForDeceleration(distance)
                        if (time > 0) {
                            action.update(-dx, -dy, time, DecelerateInterpolator(1.8f))
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
            binding.imagePlayerAvatarDetail.load(Uri.parse(player.playerImage))
            binding.siglaNome.visibility = View.GONE
        } else {
            binding.imagePlayerAvatarDetail.visibility = View.GONE
            binding.siglaNome.visibility = View.VISIBLE
            binding.siglaNome.text = player.playerName.take(2).uppercase()
        }
    }

    private fun updateCategoryButtonsForPlayer(player: PlayerState) {
        val config = mapOf(
            CategoryType.AS to (binding.valorAz to "Áz"),
            CategoryType.DUQUE to (binding.valorDuque to "Duque"),
            CategoryType.TERNO to (binding.valorTerno to "Terno"),
            CategoryType.QUADRA to (binding.valorQuadra to "Quadra"),
            CategoryType.QUINA to (binding.valorQuina to "Quina"),
            CategoryType.SENA to (binding.valorSena to "Sena"),
            CategoryType.FULL to (binding.valorFull to "Full"),
            CategoryType.SEGUIDA to (binding.valorSeguida to "Seguida"),
            CategoryType.QUADRADA to (binding.valorQuadrada to "Quadrada"),
            CategoryType.GENERAL to (binding.valorGeneral to "General")
        )

        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        val colorPrimary = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
        val colorOnPrimary = typedValue.data

        config.forEach { (type, views) ->
            val scoreEntry = player.scores[type]
            val btn = views.first
            
            if (scoreEntry != null && (scoreEntry.value > 0 || scoreEntry.isScratch)) {
                btn.alpha = 1.0f
                btn.setTextColor(colorPrimary)
                btn.setBackgroundResource(R.drawable.background_card_black)
                btn.backgroundTintList = ColorStateList.valueOf(Color.WHITE)

                if (scoreEntry.isScratch) {
                    btn.text = "" 
                    val iconSizeDp = 30
                    val iconSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconSizeDp.toFloat(), resources.displayMetrics).toInt()
                    
                    val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_riscar)
                    icon?.let {
                        it.mutate()
                        it.setTint(colorPrimary)
                        it.setBounds(0, 0, iconSizePx, iconSizePx)
                        btn.setCompoundDrawables(it, null, null, null)

                        btn.post {
                            if (isAdded && btn.width > 0) {
                                val paddingLeft = (btn.width - iconSizePx) / 2
                                btn.setPadding(paddingLeft, 0, 0, 0)
                            }
                        }
                    }
                } else {
                    val isBoca = when(type) {
                        CategoryType.FULL -> scoreEntry.value == 15
                        CategoryType.SEGUIDA -> scoreEntry.value == 25
                        CategoryType.QUADRADA -> scoreEntry.value == 35
                        CategoryType.GENERAL -> scoreEntry.value == 1000
                        else -> false
                    }

                    if (isBoca) {
                        val scoreValue = if (type == CategoryType.GENERAL) "G" else scoreEntry.value.toString()
                        val spannable = SpannableStringBuilder(" $scoreValue")
                        
                        val iconSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics).toInt()
                        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_de_boca)
                        icon?.let {
                            it.mutate()
                            it.setTint(colorPrimary)
                            it.setBounds(0, 0, iconSizePx, iconSizePx)
                            val imageSpan = ImageSpan(it, ImageSpan.ALIGN_CENTER)
                            spannable.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        
                        btn.text = spannable
                        btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        btn.setPadding(0, 0, 0, 0)
                    } else {
                        btn.text = scoreEntry.value.toString()
                        btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        btn.setPadding(0, 0, 0, 0)
                    }
                    btn.textSize = 35f
                }
            } else {
                btn.text = views.second
                btn.textSize = 20f
                btn.alpha = 0.5f
                btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                btn.setPadding(0, 0, 0, 0)
                btn.setBackgroundResource(R.drawable.background_card_black)
                btn.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                btn.setTextColor(colorPrimary)
            }
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