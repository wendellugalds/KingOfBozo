package com.wendellugalds.kingofbozo.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.BottomSheetPremiumBinding
import com.wendellugalds.kingofbozo.util.PremiumManager
import com.wendellugalds.kingofbozo.util.BillingManager

class PremiumBottomSheet(
    private val onPremiumUnlocked: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPremiumBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            val corDoPainel = ContextCompat.getColor(requireContext(), R.color.padrao_dark_nigth)

            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                val radius55 = 55f * resources.displayMetrics.density
                val shapeDrawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(corDoPainel)
                    cornerRadii = floatArrayOf(radius55, radius55, radius55, radius55, 0f, 0f, 0f, 0f)
                }
                sheet.background = shapeDrawable
            }

            bottomSheetDialog.window?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                window.navigationBarColor = corDoPainel

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = true
                }
            }
        }

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUiState()

        binding.btnUnlockPremium.setOnClickListener {
            if (PremiumManager.isUserPremium(requireContext())) {
                Toast.makeText(requireContext(), getString(R.string.premium_active_status), Toast.LENGTH_SHORT).show()
                binding.root.postDelayed(Runnable {
                    if (isAdded && !isStateSaved) {
                        dismissAllowingStateLoss()
                    }
                }, 500L)
            } else {
                BillingManager.launchBillingFlow(
                    activity = requireActivity(),
                    onSuccess = {
                        handleSuccessfulUnlock()
                    },
                    onError = { errorMsg ->
                        Toast.makeText(
                            requireContext(),
                            errorMsg.ifBlank { "Erro ao conectar com a Google Play" },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    private fun handleSuccessfulUnlock() {
        context?.let { ctx ->
            PremiumManager.setUserPremium(ctx, true)
            Toast.makeText(ctx, getString(R.string.premium_toast_unlocked), Toast.LENGTH_SHORT).show()
        }
        updateUiState()
        onPremiumUnlocked?.invoke()

        binding.root.postDelayed(Runnable {
            if (isAdded && !isStateSaved) {
                dismissAllowingStateLoss()
            }
        }, 800L)
    }

    private fun updateUiState() {
        val isPremium = PremiumManager.isUserPremium(requireContext())
        if (isPremium) {
            binding.btnUnlockPremium.text = getString(R.string.premium_active_status)
            binding.btnUnlockPremium.setIconResource(R.drawable.ic_diamond_shine)
            binding.btnUnlockPremium.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.premium)
            binding.btnUnlockPremium.setTextColor(ContextCompat.getColor(requireContext(), R.color.padrao_dark_nigth))
            binding.btnUnlockPremium.iconTint = ContextCompat.getColorStateList(requireContext(), R.color.padrao_dark_nigth)
        } else {
            binding.btnUnlockPremium.text = getString(R.string.premium_btn_unlock)
            binding.btnUnlockPremium.setIconResource(R.drawable.ic_diamond_shine)
            binding.btnUnlockPremium.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.padrao_dark)
            binding.btnUnlockPremium.setTextColor(ContextCompat.getColor(requireContext(), R.color.premium))
            binding.btnUnlockPremium.iconTint = ContextCompat.getColorStateList(requireContext(), R.color.premium)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
