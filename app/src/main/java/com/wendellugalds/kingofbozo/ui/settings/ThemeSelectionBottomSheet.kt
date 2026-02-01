package com.wendellugalds.kingofbozo.ui.settings

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

class ThemeSelectionBottomSheet(private val onThemeSelected: (Int) -> Unit) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetThemeSelectionBinding? = null
    private val binding get() = _binding!!
    private var originalNavBarColor: Int = 0
    data class ThemeOption(val name: String, val resId: Int, val colorRes: Int)

    private val themes = listOf(
        ThemeOption("Verde", R.style.Base_Theme_KingOfBozo_verde, R.color.verde),
        ThemeOption("Azul", R.style.Base_Theme_KingOfBozo_azul, R.color.azul),
        ThemeOption("Roxo", R.style.Base_Theme_KingOfBozo_roxo, R.color.roxo),
        ThemeOption("Pink", R.style.Base_Theme_KingOfBozo_pink, R.color.pink),
        ThemeOption("Amarelo", R.style.Base_Theme_KingOfBozo_amarelo, R.color.amarelo),
        ThemeOption("Laranja", R.style.Base_Theme_KingOfBozo_laranja, R.color.laranja),
        ThemeOption("Vermelho", R.style.Base_Theme_KingOfBozo_vermelho, R.color.vermelho),
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetThemeSelectionBinding.inflate(inflater, container, false)

        binding.recyclerViewThemes.adapter = ThemeAdapter(themes) { theme ->
            onThemeSelected(theme.resId)
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
            holder.colorPreview.setBackgroundResource(R.drawable.background_circle)
            holder.colorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(holder.itemView.context, theme.colorRes)
            )
            holder.itemView.setOnClickListener { onClick(theme) }
        }

        override fun getItemCount() = options.size
    }
}
