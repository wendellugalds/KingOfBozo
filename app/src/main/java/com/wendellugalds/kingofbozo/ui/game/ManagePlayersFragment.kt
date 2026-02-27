package com.wendellugalds.kingofbozo.ui.game

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.databinding.FragmentManagePlayersBinding
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.ui.AddPlayerBottomSheet

class ManagePlayersFragment : Fragment() {

    private var _binding: FragmentManagePlayersBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var inGameAdapter: ManagePlayersAdapter
    private lateinit var availableAdapter: ManagePlayersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagePlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarCoresDaBarra()

        inGameAdapter = ManagePlayersAdapter(
            onRemove = { playerName ->
                prepararTransicao()
                tryToRemovePlayer(playerName)
            }
        )
        availableAdapter = ManagePlayersAdapter(
            onAdd = { player ->
                prepararTransicao()
                gameViewModel.addPlayerToCurrentGame(player)
            }
        )

        binding.recyclerInGame.apply {
            adapter = inGameAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Desabilita animação de mudança para evitar flicker, mas mantém entrada/saída
            itemAnimator?.changeDuration = 0
        }
        binding.recyclerNoJogador.apply {
            adapter = availableAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator?.changeDuration = 0
        }

        setupDragAndDrop()

        gameViewModel.gameState.observe(viewLifecycleOwner) { state ->
            val inGamePlayers = state.playersState.map { pState ->
                Player(name = pState.playerName, age = 0, imageUri = pState.playerImage)
            }
            prepararTransicao()
            inGameAdapter.submitList(inGamePlayers, true)
            binding.totalJOGADORESLISTA.text = inGamePlayers.size.toString()
            
            updateAvailableList(inGamePlayers)
        }

        gameViewModel.availablePlayers.observe(viewLifecycleOwner) {
            val inGamePlayers = gameViewModel.gameState.value?.playersState?.map { pState ->
                Player(name = pState.playerName, age = 0, imageUri = pState.playerImage)
            } ?: emptyList()
            prepararTransicao()
            updateAvailableList(inGamePlayers)
        }

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnJogarMaisUm.setOnClickListener {
            val addPlayerSheet = AddPlayerBottomSheet()
            addPlayerSheet.show(parentFragmentManager, "AddPlayerBottomSheet")
        }

        // Ajuste de padding dinâmico para o recycler proporcional ao painel inferior
        binding.jogadoresForaJogo.post {
            if (_binding != null) {
                val height = binding.jogadoresForaJogo.height
                binding.recyclerInGame.setPadding(
                    binding.recyclerInGame.paddingLeft,
                    binding.recyclerInGame.paddingTop,
                    binding.recyclerInGame.paddingRight,
                    height
                )
            }
        }
    }

    private fun prepararTransicao() {
        val transition = AutoTransition().apply {
            duration = 300
        }
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup, transition)
    }

    private fun configurarCoresDaBarra() {
        val window = requireActivity().window
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.background)
        val corDoNavegation = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)
        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoNavegation
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, binding.root)
        val isLightBackground = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_NO
        controller.isAppearanceLightStatusBars = isLightBackground
        controller.isAppearanceLightNavigationBars = isLightBackground
    }

    private fun updateAvailableList(inGamePlayers: List<Player>) {
        val all = gameViewModel.availablePlayers.value ?: emptyList()
        val available = all.filter { p -> inGamePlayers.none { it.name == p.name } }
        availableAdapter.submitList(available, false)
        binding.totalNoJOGADORESLISTA.text = available.size.toString()
    }

    private fun tryToRemovePlayer(playerName: String) {
        if (inGameAdapter.itemCount > 2) {
            gameViewModel.removePlayerFromCurrentGame(playerName)
        } else {
            Toast.makeText(requireContext(), "O jogo deve ter pelo menos 2 jogadores.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDragAndDrop() {
        val itemTouchHelperInGame = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                
                val location = IntArray(2)
                binding.recyclerNoJogador.getLocationOnScreen(location)
                val otherListTop = location[1]
                val otherListBottom = otherListTop + binding.recyclerNoJogador.height
                
                val viewLocation = IntArray(2)
                viewHolder.itemView.getLocationOnScreen(viewLocation)
                val itemCenterY = viewLocation[1] + viewHolder.itemView.height / 2

                if (itemCenterY in otherListTop..otherListBottom) {
                    val position = viewHolder.bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        prepararTransicao()
                        val player = inGameAdapter.currentList[position]
                        tryToRemovePlayer(player.name)
                    }
                }
            }
        })

        val itemTouchHelperAvailable = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                
                val location = IntArray(2)
                binding.recyclerInGame.getLocationOnScreen(location)
                val otherListTop = location[1]
                val otherListBottom = otherListTop + binding.recyclerInGame.height
                
                val viewLocation = IntArray(2)
                viewHolder.itemView.getLocationOnScreen(viewLocation)
                val itemCenterY = viewLocation[1] + viewHolder.itemView.height / 2

                if (itemCenterY in otherListTop..otherListBottom) {
                    val position = viewHolder.bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        prepararTransicao()
                        val player = availableAdapter.currentList[position]
                        gameViewModel.addPlayerToCurrentGame(player)
                    }
                }
            }
        })

        itemTouchHelperInGame.attachToRecyclerView(binding.recyclerInGame)
        itemTouchHelperAvailable.attachToRecyclerView(binding.recyclerNoJogador)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}