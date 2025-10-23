package com.wendellugalds.kingofbozo.ui.players

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import coil.load
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentPlayersBinding
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.ui.players.adapter.PlayerAdapter
import java.lang.NumberFormatException

private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

class PlayersFragment : Fragment() {

    private var _binding: FragmentPlayersBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null

    private val playerViewModel: PlayerViewModel by activityViewModels {
        PlayerViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var playerAdapter: PlayerAdapter

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            requireActivity().contentResolver.takePersistableUriPermission(it, takeFlags)

            selectedImageUri = it
            binding.imageAvatarPreview.load(it)
            binding.imageAvatarPreview.visibility = View.VISIBLE
            binding.imageAvatarPlaceholder.visibility = View.GONE
            binding.deleteImage.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupContextualActionBar()
        setupOnBackPressed()

        playerViewModel.players.observe(viewLifecycleOwner) { players ->
            playerAdapter.submitList(players)
            updatePlayerCount(players.size)
            if (playerAdapter.isSelectionMode) {
                updateContextualActionBar()
            }
        }

        binding.deleteImage.setOnClickListener {
            selectedImageUri = null
            updateAvatarPreview()
            Toast.makeText(requireContext(), "Imagem removida.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.buttonAdicionarJogador.setOnClickListener { expandForm() }
        binding.buttonCloseForm.setOnClickListener { collapseForm() }
        binding.buttonSave.setOnClickListener { savePlayer() }
        binding.layoutAddImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

    }




    private fun setupRecyclerView() {
        playerAdapter = PlayerAdapter(
            clickListener = { player ->
                val action = PlayersFragmentDirections.actionPlayersFragmentToPlayerDetailFragment(player.id.toInt())
                findNavController().navigate(action)
            },
            selectionListener = {
                updateContextualActionBar()
            }
        )
        binding.recyclerViewPlayers.apply {
            adapter = playerAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun savePlayer() {
        val name = binding.editTextName.text.toString().trim()
        val ageString = binding.editTextAge.text.toString().trim()

        if (name.isNotEmpty() && ageString.isNotEmpty()) {
            try {
                val newPlayer = Player(
                    name = name,
                    age = ageString.toInt(),
                    imageUri = selectedImageUri?.toString()
                )
                playerViewModel.addPlayer(newPlayer)
                collapseForm()
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Por favor, insira uma idade válida.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetForm() {
        binding.editTextName.text.clear()
        binding.editTextAge.text.clear()
        selectedImageUri = null
        binding.imageAvatarPreview.visibility = View.GONE
        binding.imageAvatarPlaceholder.visibility = View.VISIBLE
        binding.deleteImage.visibility = View.GONE
    }

    private fun expandForm() {
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition())
        binding.formAddPlayer.visibility = View.VISIBLE
        binding.buttonAdicionarJogador.visibility = View.GONE
        binding.recyclerViewPlayers.visibility = View.GONE
        binding.actionBar.visibility = View.GONE
    }

    private fun collapseForm() {
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition())
        binding.formAddPlayer.visibility = View.GONE
        binding.buttonAdicionarJogador.visibility = View.VISIBLE
        binding.recyclerViewPlayers.visibility = View.VISIBLE
        binding.actionBar.visibility = View.VISIBLE
        resetForm()
    }

    private fun setupOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    playerAdapter.isSelectionMode -> {
                        playerAdapter.finishSelectionMode()
                        updateContextualActionBar()
                    }
                    binding.formAddPlayer.visibility == View.VISIBLE -> {
                        collapseForm()
                    }
                    else -> {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun setupContextualActionBar() {
        val customToolbar = binding.customToolbar
        customToolbar.actionClose.setOnClickListener {
            playerAdapter.finishSelectionMode()
            updateContextualActionBar()
        }
        customToolbar.actionDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        customToolbar.actionDeselectAll.setOnClickListener {
            playerAdapter.selectAll()
        }
    }

    private fun animateButton(button: View, hFrom: Int, hTo: Int, ptFrom: Int, ptTo: Int, pbFrom: Int, pbTo: Int) {
        val heightAnimator = ValueAnimator.ofInt(hFrom.dpToPx(requireContext()), hTo.dpToPx(requireContext()))
        heightAnimator.addUpdateListener {
            val params = button.layoutParams
            params.height = it.animatedValue as Int
            button.layoutParams = params
        }

        val paddingAnimator = ValueAnimator.ofFloat(0f, 1f)
        paddingAnimator.addUpdateListener {
            val percentage = it.animatedValue as Float
            val currentTop = (ptFrom.dpToPx(requireContext()) + ((ptTo.dpToPx(requireContext()) - ptFrom.dpToPx(requireContext())) * percentage)).toInt()
            val currentBottom = (pbFrom.dpToPx(requireContext()) + ((pbTo.dpToPx(requireContext()) - pbFrom.dpToPx(requireContext())) * percentage)).toInt()
            button.setPadding(button.paddingStart, currentTop, button.paddingEnd, currentBottom)
        }

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(heightAnimator, paddingAnimator)
        animatorSet.duration = 300
        animatorSet.start()
    }

    private fun animateRecyclerViewPadding(recyclerView: RecyclerView, from: Int, to: Int) {
        val animator = ValueAnimator.ofInt(from.dpToPx(requireContext()), to.dpToPx(requireContext()))
        animator.duration = 300
        animator.addUpdateListener { valueAnimator ->
            val newPaddingBottom = valueAnimator.animatedValue as Int
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                newPaddingBottom
            )
        }
        animator.start()
    }

    private fun updateContextualActionBar() {
        val selectedCount = playerAdapter.getSelectedItems().size
        val totalCount = playerAdapter.itemCount
        val customToolbarLayout = binding.customToolbar.root
        val actionBar = binding.actionBar
        val addPlayerButton = binding.buttonAdicionarJogador
        val recyclerView = binding.recyclerViewPlayers

        val toolbarHeight = actionBar.height.toFloat()

        if (selectedCount > 0) {
            if (customToolbarLayout.visibility == View.GONE) {
                actionBar.animate()
                    .translationY(-toolbarHeight)
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction { actionBar.visibility = View.GONE }
                    .start()

                customToolbarLayout.visibility = View.VISIBLE
                customToolbarLayout.alpha = 0f
                customToolbarLayout.translationY = -toolbarHeight
                customToolbarLayout.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .start()

                animateButton(addPlayerButton, 120, 35, 0, 25, 50, 0)
                animateRecyclerViewPadding(recyclerView, 180, 110)
            }
            binding.customToolbar.textSelectionCount.text = if (selectedCount == 1) "$selectedCount selecionado" else "$selectedCount selecionados"
        } else {
            if (customToolbarLayout.visibility == View.VISIBLE) {
                customToolbarLayout.animate()
                    .translationY(-toolbarHeight)
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        customToolbarLayout.visibility = View.GONE
                        playerAdapter.finishSelectionMode()
                    }
                    .start()

                actionBar.visibility = View.VISIBLE
                actionBar.alpha = 0f
                actionBar.translationY = -toolbarHeight
                actionBar.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .start()

                animateButton(addPlayerButton, 35, 120, 25, 0, 0, 50)
                animateRecyclerViewPadding(recyclerView, 110, 180)
            }
        }

        val deselectIcon = binding.customToolbar.actionDeselectAll
        if (selectedCount == totalCount && totalCount > 0) {
            deselectIcon.setImageResource(R.drawable.ic_deselect)
        } else {
            deselectIcon.setImageResource(R.drawable.ic_select_all)
        }
    }

    private fun showDeleteConfirmationDialog() {
        val itemsToDelete = playerAdapter.getSelectedItems()
        if (itemsToDelete.isEmpty()) {
            Toast.makeText(requireContext(), "Nenhum jogador selecionado", Toast.LENGTH_SHORT).show()
            return
        }
        val title = if (itemsToDelete.size == 1) "Apagar Jogador" else "Apagar Jogadores"
        val message = if (itemsToDelete.size == 1) "Tem certeza que deseja apagar o jogador selecionado?" else "Tem certeza que deseja apagar ${itemsToDelete.size} jogadores?"
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Apagar") { _, _ ->
                playerViewModel.deletePlayers(itemsToDelete)
                playerAdapter.finishSelectionMode()
                updateContextualActionBar()
                Toast.makeText(requireContext(), "Jogadores apagados", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updatePlayerCount(count: Int) {
        if (count > 0) {
            binding.textPlayerCount.visibility = View.VISIBLE
            binding.textPlayerCount.text = count.toString().padStart(2, '0')
        } else {
            binding.textPlayerCount.visibility = View.GONE
        }
    }

    // --- FUNÇÃO MOVIDA PARA DENTRO DA CLASSE ---
    private fun updateAvatarPreview() {
        binding.imageAvatarPreview.visibility = View.GONE
        binding.imageAvatarPlaceholder.visibility = View.VISIBLE
        binding.deleteImage.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// --- FUNÇÃO REMOVIDA DE FORA DA CLASSE ---