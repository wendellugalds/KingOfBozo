package com.wendellugalds.kingofbozo.ui.playerdetail

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentPlayerDetailBinding
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.ui.EditPlayerBottomSheet
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModel
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModelFactory
import kotlin.math.min

class PlayerDetailFragment : Fragment() {

    private var _binding: FragmentPlayerDetailBinding? = null
    private val binding get() = _binding!!

    private val playerViewModel: PlayerViewModel by activityViewModels {
        PlayerViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private val args: PlayerDetailFragmentArgs by navArgs()
    private var currentPlayer: Player? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playerId = args.playerId.toLong()

        playerViewModel.getPlayerById(playerId).observe(viewLifecycleOwner) { player ->
            if (player != null) {
                currentPlayer = player
                bind(player)
                binding.swipeRefreshLayout.isRefreshing = false
            } else {
                findNavController().popBackStack()
            }
        }

        setupClickListeners()
        configurarCoresDaBarra()
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            currentPlayer?.let { player ->
                bind(player)
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
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

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.buttonEdit.setOnClickListener {
            currentPlayer?.let {
                val editSheet = EditPlayerBottomSheet.newInstance(it.id)
                editSheet.show(parentFragmentManager, "EditPlayerSheet")
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        currentPlayer?.let { playerToDelete ->
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_player, null)
            val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setView(dialogView)
                .create()

            val title = dialogView.findViewById<TextView>(R.id.dialog_title)
            val message = dialogView.findViewById<TextView>(R.id.dialog_message)
            val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
            val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btn_delete)

            title.text = "Apagar Jogador"
            message.text = "Tem certeza que deseja apagar ${playerToDelete.name}?"

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnDelete.setOnClickListener {
                playerViewModel.deletePlayers(listOf(playerToDelete))
                Toast.makeText(requireContext(), "${playerToDelete.name} foi apagado.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun bind(player: Player) {
        binding.textPlayerNameDetail.text = player.name
        binding.textIdade.text = "${player.age} aninhos"

        if (!player.imageUri.isNullOrEmpty()) {
            binding.imagePlayerAvatarDetail.visibility = View.VISIBLE
            binding.imagePlayerAvatarDetail.load(Uri.parse(player.imageUri))
            binding.siglaNome.visibility = View.GONE
        } else {
            binding.imagePlayerAvatarDetail.visibility = View.GONE
            binding.siglaNome.visibility = View.VISIBLE

            val name = player.name.trim()
            val words = name.split(" ").filter { it.isNotBlank() }
            if (words.size > 1) {
                val firstInitial = words.first().first()
                val lastInitial = words.last().first()
                binding.siglaNome.text = "$firstInitial$lastInitial"
            } else if (words.isNotEmpty()) {
                val word = words.first()
                binding.siglaNome.text = if (word.length >= 2) word.substring(0, 2) else word
            } else {
                binding.siglaNome.text = "--"
            }
        }

        animateNumber(binding.textRankingWins, player.wins)
        animateNumber(binding.textJogadasValue, player.totalRounds)
        animateNumber(binding.textPontosRiscados, player.risksTaken)
        animateNumber(binding.textPontosBoca, player.mouthPlays)
        animateNumber(binding.textPontosAcumulados, player.totalPoints, " Pontos acumulados")
        animateNumber(binding.textGenerais, player.generals, " Generais")

        setupArcProgress(player)
        animateWeights(player.risksTaken, player.mouthPlays)
    }

    private fun animateNumber(textView: TextView, targetValue: Int, suffix: String = "") {
        ValueAnimator.ofInt(0, targetValue).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                textView.text = "${it.animatedValue}$suffix"
            }
            start()
        }
    }

    private fun animateWeights(risksTaken: Int, mouthPlays: Int) {
        val targetRisksWeight = 10f + risksTaken.toFloat()
        val targetMouthWeight = 10f + mouthPlays.toFloat()

        val paramsRiscos = binding.containerRiscos.layoutParams as LinearLayout.LayoutParams
        val paramsPontosBoca = binding.containerDeBoca.layoutParams as LinearLayout.LayoutParams

        paramsRiscos.weight = 1f
        paramsPontosBoca.weight = 1f

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                
                paramsRiscos.weight = 1f + (targetRisksWeight - 1f) * fraction
                paramsPontosBoca.weight = 1f + (targetMouthWeight - 1f) * fraction
                
                binding.containerRiscos.layoutParams = paramsRiscos
                binding.containerDeBoca.layoutParams = paramsPontosBoca
            }
            start()
        }
    }

    private fun setupArcProgress(player: Player) {
        val winsProgress = player.wins.toFloat() / 100f
        val finalProgress = min(winsProgress, 1.0f)

        val colorPrimary = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)
        val colorSurfaceVariant = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceVariant)

        val arcDrawable = object : Drawable() {
            var progress = 0f
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }

            override fun draw(canvas: Canvas) {
                val width = bounds.width().toFloat()
                val height = bounds.height().toFloat()
                if (width <= 0 || height <= 0) return

                val scaleX = width / 200f
                val scaleY = height / 120f
                val scale = min(scaleX, scaleY)
                
                val offsetX = (width - 200f * scale) / 2f
                val offsetY = (height - 120f * scale) / 2f
                
                val radius = 70f * scale
                val centerX = offsetX + 100f * scale
                val centerY = offsetY + 100f * scale
                
                val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
                paint.strokeWidth = 30f * scale

                paint.color = colorSurfaceVariant
                canvas.drawArc(rect, 180f, 180f, false, paint)

                paint.color = colorPrimary
                canvas.drawArc(rect, 180f, 180f * progress, false, paint)
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }

        binding.imageArc.setImageDrawable(arcDrawable)

        ValueAnimator.ofFloat(0f, finalProgress).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                arcDrawable.progress = it.animatedValue as Float
                arcDrawable.invalidateSelf()
            }
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
