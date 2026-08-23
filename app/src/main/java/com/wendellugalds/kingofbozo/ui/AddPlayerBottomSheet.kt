package com.wendellugalds.kingofbozo.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.BottomSheetAddPlayerBinding
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModel
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddPlayerBottomSheet : BottomSheetDialogFragment() {

    private val playerViewModel: PlayerViewModel by activityViewModels {
        val repository = (requireActivity().application as PlayersApplication).repository
        PlayerViewModelFactory(repository)
    }

    private var _binding: BottomSheetAddPlayerBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var tempImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                requireActivity().contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedImageUri = it
                updateAvatarPreview()
            } catch (e: SecurityException) {
                e.printStackTrace()
                selectedImageUri = it
                updateAvatarPreview()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedImageUri = tempImageUri
            updateAvatarPreview()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isVisible) {
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            // 1. Pega a cor EXATA da Activity (Garante que vai pegar o azul, e não o branco do Dialog)
            val typedValue = android.util.TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            val corDoPainel = typedValue.data

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                // Converte os 55dp para pixels reais da tela do celular
                val radius55 = 55f * resources.displayMetrics.density

                // Cria o fundo dinâmico
                val shapeDrawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(corDoPainel)
                    // Inicia arredondado (sem teclado)
                    cornerRadii = floatArrayOf(radius55, radius55, radius55, radius55, 0f, 0f, 0f, 0f)
                }
                it.background = shapeDrawable

                // Monitora a altura da tela para saber exatamente quando o teclado sobe ou desce
                it.viewTreeObserver.addOnGlobalLayoutListener {
                    val r = android.graphics.Rect()
                    it.getWindowVisibleDisplayFrame(r)
                    val screenHeight = it.rootView.height

                    // Calcula a altura do teclado pela diferença da tela visível
                    val keypadHeight = screenHeight - r.bottom
                    val isKeyboardVisible = keypadHeight > screenHeight * 0.15 // Se o teclado ocupa mais de 15% da tela

                    if (isKeyboardVisible) {
                        // Teclado aberto: zera os cantos superiores para ficarem retos
                        shapeDrawable.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                    } else {
                        // Teclado fechado: volta a arredondar as bordas superiores
                        shapeDrawable.cornerRadii = floatArrayOf(radius55, radius55, radius55, radius55, 0f, 0f, 0f, 0f)
                    }
                }
            }

            bottomSheetDialog.window?.let { window ->
                // 3. Remove a proteção do Android e pinta a barra com o seu azul
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                window.navigationBarColor = corDoPainel

                // 4. Força o painel a redimensionar quando o teclado subir, evitando cobrir os botões
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

                // 5. Desliga a sombra escura forçada da One UI (Samsung)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.getString("selected_image_uri")?.let { uriString ->
            selectedImageUri = Uri.parse(uriString)
            updateAvatarPreview()
        }

        binding.layoutAddImage.setOnClickListener {
            showImageSourceOptions()
        }

        binding.deleteImage.setOnClickListener {
            selectedImageUri = null
            updateAvatarPreview()
            Toast.makeText(requireContext(), "Imagem removida.", Toast.LENGTH_SHORT).show()
        }

        binding.buttonSave.setOnClickListener {
            savePlayer()
        }

        // Adiciona ação de salvar ao pressionar "Pronto" no teclado
        binding.editTextAge.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                savePlayer()
                true
            } else {
                false
            }
        }
    }

    private fun showImageSourceOptions() {
        val options = arrayOf("Câmera", "Galeria")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Selecionar Imagem")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> imagePickerLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val photoFile = createTempImageFile()
        tempImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(tempImageUri)
    }

    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("selected_image_uri", selectedImageUri?.toString())
    }

    private fun savePlayer() {
        val name = binding.editTextName.text.toString().trim()
        val ageString = binding.editTextAge.text.toString().trim()

        if (name.isEmpty() || ageString.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val newPlayer = Player(
                name = name,
                age = ageString.toInt(),
                imageUri = selectedImageUri?.toString()
            )
            playerViewModel.addPlayer(newPlayer)
            Toast.makeText(requireContext(), "$name foi adicionado!", Toast.LENGTH_SHORT).show()
            dismiss()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Por favor, insira uma idade válida.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAvatarPreview() {
        selectedImageUri?.let {
            binding.imageAvatarPreview.load(it)
            binding.imageAvatarPreview.visibility = View.VISIBLE
            binding.imageAvatarPlaceholder.visibility = View.GONE
            binding.deleteImage.visibility = View.VISIBLE
        } ?: run {
            binding.imageAvatarPreview.visibility = View.GONE
            binding.imageAvatarPlaceholder.visibility = View.VISIBLE
            binding.deleteImage.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}