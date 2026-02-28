package com.wendellugalds.kingofbozo.ui.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.wendellugalds.kingofbozo.BuildConfig
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentSettingsBinding
import com.wendellugalds.kingofbozo.ui.game.GameViewModel
import com.wendellugalds.kingofbozo.ui.game.GameViewModelFactory
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModel
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModelFactory
import com.wendellugalds.kingofbozo.util.ThemeStorage
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val playerViewModel: PlayerViewModel by activityViewModels {
        PlayerViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var countdownSeconds = 0
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (_binding != null && countdownSeconds >= 0) {
                val minutes = countdownSeconds / 60
                val seconds = countdownSeconds % 60
                
                binding.textTelaDesc.text = if (minutes > 0) {
                    String.format("APAGAR EM | %02d:%02ds", minutes, seconds)
                } else {
                    String.format("APAGAR EM | %02ds", seconds)
                }
                
                countdownSeconds--
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateThemePreview()
        updateVersionDisplay()
        configurarCoresDaBarra()
        updateNightModeDisplay()
        updateThemeName()

        binding.cardCor.setOnClickListener {
            resetCountdown()
            val bottomSheet = ThemeSelectionBottomSheet { themeKey ->
                ThemeStorage.saveTheme(requireContext(), themeKey)
                requireActivity().recreate()
            }
            bottomSheet.show(parentFragmentManager, "ThemeSelection")
        }

        binding.cardTema.setOnClickListener {
            resetCountdown()
            showNightModeDialog()
        }

        binding.cardTela.setOnClickListener {
            val isChecked = !binding.switchKeepScreenOn.isChecked
            binding.switchKeepScreenOn.isChecked = isChecked
            saveKeepScreenOn(isChecked)
        }

        binding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            saveKeepScreenOn(isChecked)
        }
        
        binding.cardReset.setOnClickListener {
            resetCountdown()
            showResetDataDialog()
        }

        binding.cardVersao.setOnClickListener {
            resetCountdown()
        }
    }

    private fun showResetDataDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_player, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val message = dialogView.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btn_delete)

        title.text = "Limpar Todos os Dados"
        message.text = "Isso apagará todos os jogos salvos e zerará todas as estatísticas de todos os jogadores. Os jogadores NÃO serão excluídos. Deseja continuar?"
        btnDelete.text = "LIMPAR TUDO"

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            resetAllData()
            dialog.dismiss()
            Toast.makeText(requireContext(), "Todos os dados foram limpos!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun resetAllData() {
        playerViewModel.viewModelScope.launch {
            playerViewModel.resetAllPlayerStats()
            gameViewModel.allSavedGames.value?.forEach { game ->
                gameViewModel.deleteSavedGame(game)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateKeepScreenOnDisplay()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(countdownRunnable)
    }

    private fun updateThemeName() {
        val currentThemeId = ThemeStorage.getTheme(requireContext())
        val themeName = when (currentThemeId) {
            R.style.Base_Theme_KingOfBozo_Standard -> "PADRÃO"
            else -> "PADRÃO"
        }
        binding.textCorDesc.text = themeName
    }

    private fun saveKeepScreenOn(enabled: Boolean) {
        ThemeStorage.saveKeepScreenOn(requireContext(), enabled)
        updateKeepScreenOnDisplay()
        applyKeepScreenOn(enabled)
    }

    private fun updateKeepScreenOnDisplay() {
        if (_binding == null) return
        val isKeepOn = ThemeStorage.getKeepScreenOn(requireContext())
        binding.switchKeepScreenOn.isChecked = isKeepOn
        
        handler.removeCallbacks(countdownRunnable)

        if (isKeepOn) {
            binding.textTelaDesc.text = "LIGADA"
        } else {
            resetCountdown()
        }
    }

    private fun resetCountdown() {
        if (_binding == null) return
        val isKeepOn = ThemeStorage.getKeepScreenOn(requireContext())
        if (isKeepOn) {
            binding.textTelaDesc.text = "LIGADA"
            handler.removeCallbacks(countdownRunnable)
            return
        }

        val timeout = try {
            Settings.System.getInt(requireContext().contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Exception) {
            -1
        }
        
        if (timeout != -1) {
            countdownSeconds = timeout / 1000
            handler.removeCallbacks(countdownRunnable)
            handler.post(countdownRunnable)
        } else {
            binding.textTelaDesc.text = "PADRÃO DO SISTEMA"
        }
    }

    private fun applyKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun showNightModeDialog() {
        val currentMode = ThemeStorage.getNightMode(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_night_mode, null)
        
        val layoutLight = dialogView.findViewById<LinearLayout>(R.id.toogle_tema_claro)
        val layoutDark = dialogView.findViewById<LinearLayout>(R.id.toogle_tema_escuro)
        val layoutSystem = dialogView.findViewById<LinearLayout>(R.id.toogle_tema_system)
        
        val switchLight = dialogView.findViewById<MaterialSwitch>(R.id.radioLight)
        val switchDark = dialogView.findViewById<MaterialSwitch>(R.id.radioDark)
        val switchSystem = dialogView.findViewById<MaterialSwitch>(R.id.radioSystem)
        
        val btnCancelar = dialogView.findViewById<Button>(R.id.btn_cancelar)

        switchLight.isChecked = currentMode == AppCompatDelegate.MODE_NIGHT_NO
        switchDark.isChecked = currentMode == AppCompatDelegate.MODE_NIGHT_YES
        switchSystem.isChecked = currentMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_KingOfBozo_AlertDialog)
            .setView(dialogView)
            .create()

        fun updateTheme(mode: Int) {
            ThemeStorage.saveNightMode(requireContext(), mode)
            updateNightModeDisplay()
            dialog.dismiss()
        }

        layoutLight.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_NO) }
        switchLight.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_NO) }

        layoutDark.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_YES) }
        switchDark.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_YES) }

        layoutSystem.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        switchSystem.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateNightModeDisplay() {
        val mode = ThemeStorage.getNightMode(requireContext())
        when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                binding.iconMode.setImageResource(R.drawable.ic_light_mode)
                binding.infoMode.text = "CLARO"
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                binding.iconMode.setImageResource(R.drawable.ic_dark_mode)
                binding.infoMode.text = "ESCURO"
            }
            else -> {
                binding.iconMode.setImageResource(R.drawable.ic_system_mode)
                binding.infoMode.text = "SISTEMA"
            }
        }
    }

    private fun updateVersionDisplay() {
        binding.textVersaoNum.text = BuildConfig.VERSION_NAME
    }

    private fun configurarCoresDaBarra() {
        val window = requireActivity().window
        // Corrected reference to R.attr.background
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.background)
        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoFundo
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, binding.root)
        val isLightBackground = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_NO
        controller.isAppearanceLightStatusBars = isLightBackground
        controller.isAppearanceLightNavigationBars = isLightBackground
    }

    private fun updateThemePreview() {
        val colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, 0)
        binding.cardCor.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPrimary)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(countdownRunnable)
        _binding = null
    }
}
