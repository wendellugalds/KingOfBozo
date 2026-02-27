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
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
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
import com.wendellugalds.kingofbozo.model.Player
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
        
        gameViewModel.gameState.value?.let { state ->
            val currentPlayer = state.playersState[state.currentPlayerIndex]
            binding.pontos.text = currentPlayer.totalScore.toString()
        }

        setupRecyclerView()
        setupCategoryButtons()
        observeGameState()
        configurarCoresDaBarra()
        setupOnBackPressed()

        binding.buttonBack.setOnClickListener { showExitConfirmationDialog() }
        
        binding.btnRakingAtual.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.marcadorFragment) {
                findNavController().navigate(R.id.action_marcadorFragment_to_rankingDuranteJogoFragment)
            }
        }

        binding.gerenciarJogadores.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.marcadorFragment) {
                findNavController().navigate(R.id.action_marcadorFragment_to_managePlayersFragment)
            }
        }
    }

    private fun showSaveGameDialog() {
        val dialogBinding = DialogSaveGameBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnSave.setOnClickListener {
            gameViewModel.saveCurrentGame()
            dialog.dismiss()
            Toast.makeText(requireContext(), "Jogo salvo!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.navigation_saved_games)
        }

        dialogBinding.btnNoSave.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.navigation_saved_games)
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
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
        binding.recyclerViewJogadores.apply {
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

    private fun animateCarJogo() {
        val carAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_car_jogo)
        val avatarAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up_avatar)
        
        binding.carJogo.startAnimation(carAnimation)
        binding.avatarContainerJogo.startAnimation(avatarAnimation)
    }

    private fun setupCategoryButtons() {
        val buttons = mapOf(
            binding.btnAz to CategoryType.AS,
            binding.btnDuque to CategoryType.DUQUE,
            binding.btnTerno to CategoryType.TERNO,
            binding.btnQuadra to CategoryType.QUADRA,
            binding.btnQuina to CategoryType.QUINA,
            binding.btnSena to CategoryType.SENA,
            binding.btnFull to CategoryType.FULL,
            binding.btnSeguida to CategoryType.SEGUIDA,
            binding.btnQuadrada to CategoryType.QUADRADA,
            binding.btnGeneral to CategoryType.GENERAL
        )

        buttons.forEach { (view, type) ->
            view.setOnClickListener { openScoringBottomSheet(type, view as TextView) }
        }
    }

    private fun openScoringBottomSheet(type: CategoryType, btnView: TextView) {
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

        setupSheetValueClicks(sheetView, dialog, type, btnView)
        setupInfoAnimation(sheetView)
        
        dialog.show()
    }

    private fun setupInfoAnimation(view: View) {
        val buttonInfo = view.findViewById<View>(R.id.button_info) ?: return
        val infoDados = view.findViewById<LinearLayout>(R.id.info_dados) ?: return

        buttonInfo.setOnClickListener {
            if (infoDados.visibility == View.GONE) {
                showInfoWithAnimation(buttonInfo, infoDados)
            } else {
                hideInfoWithAnimation(buttonInfo, infoDados)
            }
        }
    }

    private fun showInfoWithAnimation(buttonInfo: View, infoDados: LinearLayout) {
        infoDados.visibility = View.VISIBLE
        infoDados.post {
            val buttonLocation = IntArray(2)
            buttonInfo.getLocationOnScreen(buttonLocation)
            val buttonCenterX = buttonLocation[0] + buttonInfo.width / 2
            val buttonCenterY = buttonLocation[1] + buttonInfo.height / 2

            for (i in 0 until infoDados.childCount) {
                val child = infoDados.getChildAt(i)
                val childLocation = IntArray(2)
                child.getLocationOnScreen(childLocation)

                val childCenterX = childLocation[0] + child.width / 2
                val childCenterY = childLocation[1] + child.height / 2

                child.translationX = (buttonCenterX - childCenterX).toFloat()
                child.translationY = (buttonCenterY - childCenterY).toFloat()
                child.alpha = 0f
                child.scaleX = 0f
                child.scaleY = 0f

                child.animate()
                    .translationX(0f)
                    .translationY(0f)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setStartDelay(i * 50L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun hideInfoWithAnimation(buttonInfo: View, infoDados: LinearLayout) {
        val buttonLocation = IntArray(2)
        buttonInfo.getLocationOnScreen(buttonLocation)
        val buttonCenterX = buttonLocation[0] + buttonInfo.width / 2
        val buttonCenterY = buttonLocation[1] + buttonInfo.height / 2

        val totalCount = infoDados.childCount
        var finishedCount = 0

        for (i in 0 until totalCount) {
            val child = infoDados.getChildAt(i)
            val childLocation = IntArray(2)
            child.getLocationOnScreen(childLocation)

            val childCenterX = childLocation[0] + child.width / 2
            val childCenterY = childLocation[1] + child.height / 2

            child.animate()
                .translationX((buttonCenterX - childCenterX).toFloat())
                .translationY((buttonCenterY - childCenterY).toFloat())
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(400)
                .setStartDelay((totalCount - 1 - i) * 50L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    finishedCount++
                    if (finishedCount == totalCount) {
                        infoDados.visibility = View.GONE
                    }
                }
                .start()
        }
    }

    private fun setupSheetValueClicks(view: View, dialog: BottomSheetDialog, type: CategoryType, btnView: TextView) {
        val values = listOf(R.id.valor_01, R.id.valor_02, R.id.valor_03, R.id.valor_04, R.id.valor_05, R.id.valor_boca)
        
        view.findViewById<View>(R.id.btn_nulo)?.setOnClickListener {
            updateScore(type, 0, null, btnView, shouldAutoAdvance = false, isClear = true)
            dialog.dismiss()
        }

        view.findViewById<TextView>(R.id.btn_riscar)?.setOnClickListener {
            updateScore(type, 0, "X", btnView, isScratch = true)
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.valor_ganhou_boca)?.setOnClickListener {
            showConfirmBocaDialog(type, btnView, dialog)
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
                
                updateScore(type, score, score.toString(), btnView)
                dialog.dismiss()
            }
        }
        
        view.findViewById<View>(R.id.button_cancel)?.setOnClickListener { dialog.dismiss() }
    }

    private fun showConfirmBocaDialog(type: CategoryType, btnView: TextView, parentDialog: BottomSheetDialog) {
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
            updateScore(type, score, score.toString(), btnView)
            
            alertDialog.dismiss()
            parentDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun updateScore(type: CategoryType, score: Int, display: String?, btnView: TextView, isScratch: Boolean = false, shouldAutoAdvance: Boolean = true, isClear: Boolean = false) {
        gameViewModel.submitScore(type, score, isScratch, shouldAutoAdvance, isClear)
    }

    private fun animateScoreChange(start: Int, end: Int) {
        scoreAnimator?.cancel()
        scoreAnimator = ValueAnimator.ofInt(start, end).apply {
            duration = 600
            addUpdateListener { valueAnimator ->
                _binding?.let {
                    it.pontos.text = valueAnimator.animatedValue.toString()
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
                    binding.pontos.text = lastTotalScore.toString()
                } else if (it.currentPlayerIndex != lastPlayerIndex) {
                    lastPlayerIndex = it.currentPlayerIndex
                    animateCarJogo()
                    
                    scoreAnimator?.cancel()
                    animateScoreChange(0, currentPlayer.totalScore)
                    lastTotalScore = currentPlayer.totalScore
                } else if (currentPlayer.totalScore != lastTotalScore) {
                    scoreAnimator?.cancel()
                    binding.pontos.text = currentPlayer.totalScore.toString()
                    lastTotalScore = currentPlayer.totalScore
                }

                val players = it.playersState
                val maxScore = players.maxOfOrNull { p -> p.totalScore } ?: 0
                val winnersCount = players.count { p -> p.totalScore == maxScore && maxScore > 0 }
                
                binding.btnRakingAtual.isVisible = players.any { p -> p.totalScore > 0 }

                val currentFirstName = currentPlayer.playerName.split(Regex("\\s+")).firstOrNull() ?: ""
                
                val spannable = SpannableStringBuilder()

                val tiedPlayers = players.filter { it.totalScore == currentPlayer.totalScore && it.totalScore > 0 }

                if (tiedPlayers.size > 1) {
                    val others = tiedPlayers.filter { it.playerName != currentPlayer.playerName }
                    spannable.append(currentPlayer.playerName.substringBefore(" "))

                    others.forEach { player ->
                        val firstName = player.playerName.substringBefore(" ")
                        spannable.append("  ")
                        val iconStart = spannable.length - 2

                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_empate)?.mutate()?.apply {
                            val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics).toInt()
                            setTint(MaterialColors.getColor(binding.root, com.google.android.material.R.attr.iconTint))
                            setBounds(20, 0, size, size)

                            spannable.setSpan(ImageSpan(this, ImageSpan.ALIGN_CENTER), iconStart, iconStart + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        spannable.append(firstName)
                    }
                    binding.iconEmpateNumbers.isVisible = true
                } else {
                    spannable.append(currentFirstName)
                    binding.iconEmpateNumbers.isVisible = false
                }
                
                binding.nomeJogador.text = spannable
                
                updatePlayerAvatar(currentPlayer, maxScore, winnersCount)

                if (isFirstLoad) {
                    isFirstLoad = true
                    playerMarkerAdapter.updateState(it.currentPlayerIndex, it.playersState)
                    scrollToPositionCentered(it.currentPlayerIndex, isImmediate = false)
                } else {
                    scrollToPositionCentered(it.currentPlayerIndex, isImmediate = false) {
                        playerMarkerAdapter.updateState(it.currentPlayerIndex, it.playersState)
                    }
                }

                binding.textTitleJogadores.text = "RODADA ${it.currentRound.toString().padStart(2, '0')}"
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
        val recyclerView = binding.recyclerViewJogadores
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

    private fun updatePlayerAvatar(player: PlayerState, maxScore: Int, winnersCount: Int) {
        if (!player.playerImage.isNullOrEmpty()) {
            binding.imagePlayerAvatarJogo.visibility = View.VISIBLE
            binding.imagePlayerAvatarJogo.load(Uri.parse(player.playerImage))
            binding.siglaNome.visibility = View.GONE
        } else {
            binding.imagePlayerAvatarJogo.visibility = View.GONE
            binding.siglaNome.visibility = View.VISIBLE
            binding.siglaNome.text = player.playerName.take(2).uppercase()
        }

        val isWinning = player.totalScore == maxScore && maxScore > 0
        if (isWinning) {
            if (winnersCount > 1) {
                binding.iconKing.isVisible = false
            } else {
                binding.iconKing.isVisible = true
            }
        } else {
            binding.iconKing.isVisible = false
        }
    }

    private fun updateCategoryButtonsForPlayer(player: PlayerState) {
        val config = mapOf(
            CategoryType.AS to (binding.btnAz to "Àz"),
            CategoryType.DUQUE to (binding.btnDuque to "Duque"),
            CategoryType.TERNO to (binding.btnTerno to "Terno"),
            CategoryType.QUADRA to (binding.btnQuadra to "Quadra"),
            CategoryType.QUINA to (binding.btnQuina to "Quina"),
            CategoryType.SENA to (binding.btnSena to "Sena"),
            CategoryType.FULL to (binding.btnFull to "Full"),
            CategoryType.SEGUIDA to (binding.btnSeguida to "Seguida"),
            CategoryType.QUADRADA to (binding.btnQuadrada to "Quadrada"),
            CategoryType.GENERAL to (binding.btnGeneral to "General")
        )

        val typedValue = TypedValue()
        val theme = requireContext().theme
        val corfixa = Color.parseColor("#FFFFFF")

        theme.resolveAttribute(com.google.android.material.R.attr.textAppearanceButton, typedValue, true)
        val colorTextBackground = typedValue.data
        
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        val colorPrimary = typedValue.data

        config.forEach { (type, views) ->
            val scoreEntry = player.scores[type]
            val btn = views.first
            
            if (scoreEntry != null && (scoreEntry.value > 0 || scoreEntry.isScratch)) {
                btn.alpha = 1.0f
                
                if (scoreEntry.isScratch) {
                    btn.text = "" 
                    val iconSizeDp = 35 
                    val iconSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconSizeDp.toFloat(), resources.displayMetrics).toInt()
                    
                    btn.setBackgroundResource(R.drawable.background_score_option_riscar)
                    btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    
                    val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_riscar)
                    icon?.let {
                        it.setTint(corfixa)
                        it.setBounds(0, 0, iconSizePx, iconSizePx)
                        btn.setCompoundDrawables(it, null, null, null)

                        btn.post {
                            if (isAdded && btn.width > 0) {
                                val paddingLeft = (btn.width - iconSizePx) / 2
                                btn.setPadding(paddingLeft, 0, 0, 0)
                            }
                        }
                    }
                    btn.backgroundTintList = ColorStateList.valueOf(corfixa)
                } else {
                    val isBoca = when(type) {
                        CategoryType.FULL -> scoreEntry.value == 15
                        CategoryType.SEGUIDA -> scoreEntry.value == 25
                        CategoryType.QUADRADA -> scoreEntry.value == 35
                        else -> false
                    }

                    if (isBoca) {
                        val scoreText = scoreEntry.value.toString()
                        val spannable = SpannableStringBuilder(" $scoreText")
                        
                        val iconSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25f, resources.displayMetrics).toInt()
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
                    } else {
                        btn.text = scoreEntry.value.toString()
                        btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    }

                    btn.textSize = 45f
                    btn.setPadding(0, 0, 0, 0)
                    btn.setBackgroundResource(R.drawable.background_card_black)
                    btn.backgroundTintList = ColorStateList.valueOf(corfixa)
                    btn.setTextColor(colorPrimary)
                }
            } else {
                btn.text = views.second
                btn.textSize = 20f
                btn.alpha = 0.5f
                btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                btn.setPadding(0, 0, 0, 0)
                btn.setBackgroundResource(R.drawable.background_card_black)
                btn.backgroundTintList = ColorStateList.valueOf(corfixa)
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