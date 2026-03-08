package com.wendellugalds.kingofbozo.ui.settings

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.BottomSheetThemeSelectionBinding

class ThemeSelectionBottomSheet(private val onThemeSelected: (String) -> Unit) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetThemeSelectionBinding? = null
    private val binding get() = _binding!!
    private var originalNavBarColor: Int = 0
    data class ThemeOption(val name: String, val themeKey: String, val colorRes: Int, val colorResNight: Int)

    private val themes = listOf(
        ThemeOption("Padrão", "PADRAO", R.color.padrao, R.color.padrao_night),
        ThemeOption("Verde", "VERDE", R.color.verde, R.color.verde_night),
        ThemeOption("Roxo", "ROXO", R.color.roxo, R.color.roxo_night),
        ThemeOption("Rosa", "Rosa", R.color.rosa, R.color.rosa_night),
        ThemeOption("Laranja", "LARANJA", R.color.laranja, R.color.laranja_night),
        ThemeOption("Vermelho", "VERMELHO", R.color.vermelho, R.color.vermelho_night)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetThemeSelectionBinding.inflate(inflater, container, false)

        binding.recyclerViewThemes.adapter = ThemeAdapter(themes) { theme ->
            onThemeSelected(theme.themeKey)
            dismiss()
        }
        binding.recyclerViewThemes.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

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

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                originalNavBarColor = requireActivity().window.navigationBarColor
                val corDoTema = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.cardBackgroundColor)
                window.navigationBarColor = corDoTema
            }
        }
    }

    class ThemeAdapter(private val options: List<ThemeOption>, private val onClick: (ThemeOption) -> Unit) :
        RecyclerView.Adapter<ThemeAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val colorPreview: View = view.findViewById(R.id.view_color_preview)
            val nameText: TextView = view.findViewById(R.id.text_theme_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_theme_selection, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val theme = options[position]
            holder.nameText.text = theme.name
            holder.colorPreview.setBackgroundResource(R.drawable.celular_cor)

            val context = holder.itemView.context
            val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val colorRes = if (isNightMode) theme.colorResNight else theme.colorRes

            holder.colorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(context, colorRes)
            )
            holder.itemView.setOnClickListener { onClick(theme) }
        }

        override fun getItemCount() = options.size
    }
}
