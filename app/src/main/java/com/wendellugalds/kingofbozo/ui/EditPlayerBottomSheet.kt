package com.wendellugalds.kingofbozo.ui

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.BottomSheetEditPlayerBinding
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModel
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModelFactory

class EditPlayerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditPlayerBinding? = null
    private val binding get() = _binding!!

    // ... (outras variáveis continuam iguais)
    private val playerViewModel: PlayerViewModel by activityViewModels {
        PlayerViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }
    private var playerToEdit: Player? = null
    private var originalNavBarColor: Int = 0
    private var currentImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                requireActivity().contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                currentImageUri = it
                updateAvatarPreview()
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Erro de permissão para a imagem.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetEditPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        savedInstanceState?.getString("current_image_uri")?.let { uriString ->
            currentImageUri = Uri.parse(uriString)
        }

        val playerId = arguments?.getLong("PLAYER_ID") ?: return

        playerViewModel.getPlayerById(playerId).observe(viewLifecycleOwner) { player ->
            player?.let {
                playerToEdit = it
                if (binding.editTextName.text.isEmpty()) {
                    binding.editTextName.setText(it.name)
                    binding.editTextAge.setText(it.age.toString())
                }
                if (currentImageUri == null) {
                    it.imageUri?.let { uriString ->
                        currentImageUri = Uri.parse(uriString)
                    }
                }
                updateAvatarPreview()
            }
        }

        binding.buttonSave.setOnClickListener {
            updatePlayer()
        }

        binding.layoutAddImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // --- LÓGICA DO BOTÃO DELETAR ADICIONADA ---
        binding.deleteImage.setOnClickListener {
            currentImageUri = null // Remove a referência da imagem
            updateAvatarPreview() // Atualiza a UI para mostrar o placeholder
            Toast.makeText(requireContext(), "Imagem removida.", Toast.LENGTH_SHORT).show()
        }
    }

    // ... (funções onSaveInstanceState e updatePlayer continuam iguais)

    // --- FUNÇÃO ATUALIZADA PARA CONTROLAR A VISIBILIDADE DO BOTÃO ---
    private fun updateAvatarPreview() {
        if (currentImageUri != null) {
            binding.imageAvatarPreview.load(currentImageUri)
            binding.imageAvatarPreview.visibility = View.VISIBLE
            binding.imageAvatarPlaceholder.visibility = View.GONE
            binding.deleteImage.visibility = View.VISIBLE // MOSTRA o botão de deletar
        } else {
            binding.imageAvatarPreview.visibility = View.GONE
            binding.imageAvatarPlaceholder.visibility = View.VISIBLE
            binding.deleteImage.visibility = View.GONE // ESCONDE o botão de deletar
        }
    }

    // ... (resto do arquivo, como onStart, onDismiss, etc. continua igual) ...
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_image_uri", currentImageUri?.toString())
    }

    private fun updatePlayer() {
        val currentName = binding.editTextName.text.toString().trim()
        val currentAgeStr = binding.editTextAge.text.toString().trim()

        if (currentName.isEmpty() || currentAgeStr.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        playerToEdit?.let { player ->
            val updatedPlayer = player.copy(
                name = currentName,
                age = currentAgeStr.toInt(),
                imageUri = currentImageUri?.toString()
            )
            playerViewModel.updatePlayer(updatedPlayer)
            Toast.makeText(requireContext(), "Jogador atualizado!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                originalNavBarColor = requireActivity().window.navigationBarColor
                window.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.cor_destaque)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requireActivity().window.navigationBarColor = originalNavBarColor
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(playerId: Long): EditPlayerBottomSheet {
            val args = Bundle()
            args.putLong("PLAYER_ID", playerId)
            val fragment = EditPlayerBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }
}