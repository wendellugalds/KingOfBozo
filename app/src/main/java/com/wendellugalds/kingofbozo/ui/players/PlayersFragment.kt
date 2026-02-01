package com.wendellugalds.kingofbozo.ui.players

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.DialogDeletePlayerBinding
import com.wendellugalds.kingofbozo.databinding.FragmentPlayersBinding
import com.wendellugalds.kingofbozo.ui.AddPlayerBottomSheet
import com.wendellugalds.kingofbozo.ui.players.adapter.PlayerAdapter


private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

class PlayersFragment : Fragment() {

    private var _binding: FragmentPlayersBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null

    private val playerViewModel: PlayerViewModel by activityViewModels {
        PlayerViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var playerAdapter: PlayerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupContextualActionBar()
        setupOnBackPressed()

        playerViewModel.players.observe(viewLifecycleOwner) { players ->
            playerAdapter.submitList(players)
            updatePlayerCount(players.size)
            binding.personSelect.visibility = if (players.size > 1) View.VISIBLE else View.GONE
            if (playerAdapter.isSelectionMode) {
                updateContextualActionBar()
            }
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.buttonAdicionarJogador.setOnClickListener {
            val addSheet = AddPlayerBottomSheet()
            addSheet.show(parentFragmentManager, "AddPlayerSheet")
        }

        binding.personSelect.setOnClickListener {
            if (playerAdapter.isSelectionMode) {
                playerAdapter.finishSelectionMode()
            } else {
                playerAdapter.startSelectionMode()
            }
            updateContextualActionBar()
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

    private fun setupOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    playerAdapter.isSelectionMode -> {
                        playerAdapter.finishSelectionMode()
                        updateContextualActionBar()
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
            playerAdapter.finishSelectionMode()
            updateContextualActionBar()
        }
    }

    private fun animateButton(button: View, hFrom: Int, hTo: Int, mbFrom: Int, mbTo: Int, ptFrom: Int, ptTo: Int, pbFrom: Int, pbTo: Int) {
        val heightAnimator = ValueAnimator.ofInt(hFrom.dpToPx(requireContext()), hTo.dpToPx(requireContext()))
        heightAnimator.addUpdateListener {
            val params = button.layoutParams as ViewGroup.MarginLayoutParams
            params.height = it.animatedValue as Int
            button.layoutParams = params
        }

        val marginAnimator = ValueAnimator.ofInt(mbFrom.dpToPx(requireContext()), mbTo.dpToPx(requireContext()))
        marginAnimator.addUpdateListener {
            val params = button.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = it.animatedValue as Int
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
        animatorSet.playTogether(heightAnimator, marginAnimator, paddingAnimator)
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

        if (playerAdapter.isSelectionMode) {
            binding.personSelect.setImageResource(R.drawable.ic_person_check)
            binding.personSelect.visibility = View.GONE
//            binding.personSelect.setImageResource(R.drawable.ic_deselect)
        }else {
            binding.personSelect.setImageResource(R.drawable.ic_person_check)
            binding.personSelect.visibility = View.VISIBLE
//            binding.personSelect.setImageResource(R.drawable.ic_person_check)
     }

        if (playerAdapter.isSelectionMode) {
            addPlayerButton.isEnabled = false
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

                // h: 150->82, mb: 20->20, pt: 25->25, pb: 0->0
                animateButton(addPlayerButton, 150, 82, 20, 20, 25, 25, 0, 0)
                animateRecyclerViewPadding(recyclerView, 180, 110)
                addPlayerButton.setBackgroundResource(R.drawable.background_card_dark)
            }
            binding.customToolbar.textSelectionCount.text = if (selectedCount == 1) "$selectedCount selecionado" else "$selectedCount selecionados"
        } else {
            addPlayerButton.isEnabled = true
            if (customToolbarLayout.visibility == View.VISIBLE) {
                customToolbarLayout.animate()
                    .translationY(-toolbarHeight)
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        customToolbarLayout.visibility = View.GONE
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

                // h: 82->150, mb: 20->20, pt: 25->25, pb: 0->0
                animateButton(addPlayerButton, 82, 150, 20, 20, 25, 25, 0, 0)
                animateRecyclerViewPadding(recyclerView, 110, 180)
                addPlayerButton.setBackgroundResource(R.drawable.background_card_2_destaque)
            }
        }

        val deselectIcon = binding.customToolbar.actionDeselectAll
        deselectIcon.setImageResource(R.drawable.ic_deselect)
    }

    private fun showDeleteConfirmationDialog() {
        val itemsToDelete = playerAdapter.getSelectedItems()
        if (itemsToDelete.isEmpty()) {
            Toast.makeText(requireContext(), "Nenhum jogador selecionado", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogDeletePlayerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.dialogTitle.text = if (itemsToDelete.size == 1) "Apagar Jogador" else "Apagar Jogadores"
        dialogBinding.dialogMessage.text = if (itemsToDelete.size == 1) 
            "Tem certeza que deseja apagar o jogador selecionado?" 
            else "Tem certeza que deseja apagar ${itemsToDelete.size} jogadores?"

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDelete.setOnClickListener {
            playerViewModel.deletePlayers(itemsToDelete)
            playerAdapter.finishSelectionMode()
            updateContextualActionBar()
            Toast.makeText(requireContext(), "Jogadores apagados", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun updatePlayerCount(count: Int) {
        if (count > 0) {
            binding.textPlayerCount.visibility = View.VISIBLE
            binding.textPlayerCount.text = count.toString().padStart(2, '0')
        } else {
            binding.textPlayerCount.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
