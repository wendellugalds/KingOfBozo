package com.wendellugalds.kingofbozo.ui.game

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentMarkerBinding
import com.wendellugalds.kingofbozo.model.Category
import com.wendellugalds.kingofbozo.model.CategoryType
import com.wendellugalds.kingofbozo.ui.game.adapter.CategoryAdapter
import com.wendellugalds.kingofbozo.ui.game.adapter.PlayerMarkerAdapter
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModel
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModelFactory
import kotlin.math.abs

class MarcadorFragment : Fragment() {

    private var _binding: FragmentMarkerBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }
    private val playerViewModel: PlayerViewModel by activityViewModels {
        PlayerViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }
    private val args: MarcadorFragmentArgs by navArgs()

    private lateinit var playerMarkerAdapter: PlayerMarkerAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var currentScoringView: View? = null
    private var previousPlayerIndex: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMarkerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupPlayerCarousel()

        val selectedIds = args.selectedPlayerIds.toList()

        playerViewModel.players.observe(viewLifecycleOwner) { allPlayers ->
            val playersMap = allPlayers.associateBy { it.id }
            val selectedPlayersInOrder = selectedIds.mapNotNull { id -> playersMap[id] }
            playerMarkerAdapter.submitList(selectedPlayersInOrder)
        }

        gameViewModel.gameState.observe(viewLifecycleOwner) { gameState ->
            gameState?.let {
                val newPlayerIndex = it.currentPlayerIndex
                val didPlayerChange =
                    previousPlayerIndex != -1 && previousPlayerIndex != newPlayerIndex

                binding.textRound.text = it.currentRound.toString().padStart(2, '0')

                playerMarkerAdapter.setCurrentPlayerIndex(newPlayerIndex)

                if (didPlayerChange) {
                    updateCategoriesWithAnimation(newPlayerIndex)
                    binding.playersMarcador.smoothScrollToPosition(newPlayerIndex)
                } else if (previousPlayerIndex == -1) {
                    updateCategoriesWithoutAnimation()
                }

                playerMarkerAdapter.updateGameState(it)
                previousPlayerIndex = newPlayerIndex
            }
        }

        gameViewModel.openedCategory.observe(viewLifecycleOwner) { category ->
            currentScoringView?.let { binding.categoriesContainer.removeView(it) }
            currentScoringView = null

            if (category != null) {
                binding.gridCategories.visibility = View.GONE
                binding.playersMarcador.isLayoutFrozen = true
                inflateAndShowScoringUi(category)
            } else {
                binding.gridCategories.visibility = View.VISIBLE
                binding.playersMarcador.isLayoutFrozen = false
            }
        }

        binding.buttonBack.setOnClickListener {
            if (gameViewModel.openedCategory.value != null) {
                gameViewModel.closeCategoryDetail()
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun setupPlayerCarousel() {
        playerMarkerAdapter = PlayerMarkerAdapter()
        binding.playersMarcador.adapter = playerMarkerAdapter

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.playersMarcador)

        binding.playersMarcador.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(layoutManager)
                    centerView?.let {
                        val position = layoutManager.getPosition(it)
                        if (previousPlayerIndex != position) {
                            gameViewModel.setCurrentPlayer(position)
                            updateCategoriesWithAnimation(position)
                        }
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                applyCarouselTransformations()
            }
        })
        binding.playersMarcador.post { applyCarouselTransformations() }
    }

    private fun applyCarouselTransformations() {
        val centerX = binding.playersMarcador.width / 2f

        for (i in 0 until binding.playersMarcador.childCount) {
            val child = binding.playersMarcador.getChildAt(i)
            val childCenterX = (child.left + child.right) / 2f
            val distance = abs(centerX - childCenterX)

            val scale = Math.max(0.75f, 1f - 0.25f * (distance / centerX))
            child.scaleX = scale
            child.scaleY = scale
        }
    }

    private fun updateCategoriesWithoutAnimation() {
        categoryAdapter.submitList(gameViewModel.getCategoriesForCurrentPlayer())
    }

    private fun updateCategoriesWithAnimation(newPlayerIndex: Int) {
        val oldPlayerIndex = previousPlayerIndex
        if (oldPlayerIndex == -1 || oldPlayerIndex == newPlayerIndex) return

        val slideOutAnimRes =
            if (newPlayerIndex > oldPlayerIndex) R.anim.slide_out_left else R.anim.slide_out_right
        val slideInAnimRes =
            if (newPlayerIndex > oldPlayerIndex) R.anim.slide_in_right else R.anim.slide_in_left
        animateCategoryContainer(slideOutAnimRes) {
            categoryAdapter.submitList(gameViewModel.getCategoriesForCurrentPlayer())
            animateCategoryContainer(slideInAnimRes)
        }
    }

    private fun inflateAndShowScoringUi(category: Category) {
        val layoutId = when (category.type) {
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
        val inflater = LayoutInflater.from(requireContext())
        val scoringView = inflater.inflate(layoutId, binding.categoriesContainer, false)
        setupScoringClickListeners(scoringView, category)
        binding.categoriesContainer.addView(scoringView)
        currentScoringView = scoringView
    }

    private fun setupScoringClickListeners(view: View, category: Category) {
        fun submitScoreAndAdvance(score: Int, isScratch: Boolean = false) {
            val currentIndex = gameViewModel.gameState.value?.currentPlayerIndex ?: 0
            val totalPlayers = playerMarkerAdapter.itemCount

            gameViewModel.submitScore(category.type, score, isScratch)

            if (totalPlayers > 0) {
                val nextIndex = (currentIndex + 1) % totalPlayers
                binding.playersMarcador.smoothScrollToPosition(nextIndex)
            }
        }

        view.findViewById<ImageView>(R.id.button_cancel)
            ?.setOnClickListener { gameViewModel.closeCategoryDetail() }
        view.findViewById<ImageView>(R.id.btn_nulo)?.setOnClickListener { submitScoreAndAdvance(0) }
        view.findViewById<TextView>(R.id.btn_riscar)
            ?.setOnClickListener { submitScoreAndAdvance(0, isScratch = true) }

        when (category.type) {
            CategoryType.AS, CategoryType.DUQUE, CategoryType.TERNO, CategoryType.QUADRA, CategoryType.QUINA, CategoryType.SENA -> {
                val multiplier = when (category.type) {
                    CategoryType.AS -> 1; CategoryType.DUQUE -> 2; CategoryType.TERNO -> 3
                    CategoryType.QUADRA -> 4; CategoryType.QUINA -> 5; CategoryType.SENA -> 6
                    else -> 0
                }
                view.findViewById<TextView>(R.id.valor_01)
                    ?.setOnClickListener { submitScoreAndAdvance(1 * multiplier) }
                view.findViewById<TextView>(R.id.valor_02)
                    ?.setOnClickListener { submitScoreAndAdvance(2 * multiplier) }
                view.findViewById<TextView>(R.id.valor_03)
                    ?.setOnClickListener { submitScoreAndAdvance(3 * multiplier) }
                view.findViewById<TextView>(R.id.valor_04)
                    ?.setOnClickListener { submitScoreAndAdvance(4 * multiplier) }
                view.findViewById<TextView>(R.id.valor_05)
                    ?.setOnClickListener { submitScoreAndAdvance(5 * multiplier) }
            }

            CategoryType.FULL -> {
                view.findViewById<TextView>(R.id.valor_01)
                    ?.setOnClickListener { submitScoreAndAdvance(resources.getInteger(R.integer.score_full_normal)) }
                view.findViewById<TextView>(R.id.valor_boca)
                    ?.setOnClickListener { submitScoreAndAdvance(resources.getInteger(R.integer.score_full_boca)) }
            }

            CategoryType.SEGUIDA -> {
                view.findViewById<TextView>(R.id.valor_01)
                    ?.setOnClickListener { submitScoreAndAdvance(resources.getInteger(R.integer.score_seguida_normal)) }
                view.findViewById<TextView>(R.id.valor_boca)
                    ?.setOnClickListener { submitScoreAndAdvance(resources.getInteger(R.integer.score_seguida_boca)) }
            }

            CategoryType.QUADRADA -> {
                view.findViewById<TextView>(R.id.valor_01)
                    ?.setOnClickListener { submitScoreAndAdvance(resources.getInteger(R.integer.score_quadrada_normal)) }
                view.findViewById<TextView>(R.id.valor_boca)
                    ?.setOnClickListener { submitScoreAndAdvance(resources.getInteger(R.integer.score_quadrada_boca)) }
            }

            CategoryType.GENERAL -> {
                view.findViewById<TextView>(R.id.valor_01)
                    ?.setOnClickListener { submitScoreAndAdvance(resources.getInteger(R.integer.score_general_normal)) }
                view.findViewById<TextView>(R.id.valor_ganhou_boca)
                    ?.setOnClickListener { submitScoreAndAdvance(resources.getInteger(R.integer.score_general_vitoria)) }
            }
        }
    }

    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter { category ->
            if (category.isScored) {
                gameViewModel.resetScore(category.type)
            } else {
                gameViewModel.openCategoryDetail(category)
            }
        }
        binding.gridCategories.apply {
            val totalDeColunas = 3
            val gridLayoutManager = GridLayoutManager(requireContext(), totalDeColunas)
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position == categoryAdapter.itemCount - 1) totalDeColunas else 1
                }
            }
            adapter = categoryAdapter
            layoutManager = gridLayoutManager
        }

        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.playersMarcador.layoutManager = layoutManager
    }

    private fun animateCategoryContainer(animationResId: Int, onEnd: (() -> Unit)? = null) {
        val animation = AnimationUtils.loadAnimation(requireContext(), animationResId)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                onEnd?.invoke()
            }
        })
        binding.categoriesContainer.startAnimation(animation)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}