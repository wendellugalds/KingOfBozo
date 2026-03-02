package com.wendellugalds.kingofbozo.ui.game.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.ItemCategoryMarkerBinding
import com.wendellugalds.kingofbozo.model.Category

class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var categories: List<Category> = emptyList()

    fun submitList(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    companion object {
        fun applyCategoryStyle(
            textView: TextView,
            root: View,
            categoryName: String,
            score: Int?,
            isScored: Boolean,
            isScratch: Boolean,
            isBoca: Boolean,
            context: Context
        ) {
            val colorPrimary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.BLUE)
            val colorWhite = Color.WHITE

            // RESET PADRÃO
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            textView.setPadding(0, 0, 0, 0)
            textView.compoundDrawablePadding = 0
            textView.alpha = 1.0f
            textView.setTypeface(null, Typeface.BOLD)

            if (isScored || isScratch) {
                // ESTADO PONTUADO: Fundo Branco, Texto Primary, Sem Alpha
                textView.setTextColor(colorPrimary)
                root.backgroundTintList = ColorStateList.valueOf(colorWhite)
                root.setBackgroundResource(R.drawable.background_card_black)

                when {
                    isScratch -> {
                        textView.text = ""
                        root.setBackgroundResource(R.drawable.background_score_option_riscar)
                        root.backgroundTintList = null
                        val icon = ContextCompat.getDrawable(context, R.drawable.ic_riscar)
                        icon?.mutate()?.setTint(Color.WHITE)
                        textView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
                        
                        root.post {
                            if (root.width > 0) {
                                val iconSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, context.resources.displayMetrics).toInt()
                                textView.setPadding((root.width - iconSizePx) / 2, 0, 0, 0)
                            }
                        }
                    }
                    isBoca -> {
                        val scoreValue = if (categoryName == "General") "G" else score.toString()
                        textView.text = scoreValue
                        textView.textSize = 45f
                        val icon = ContextCompat.getDrawable(context, R.drawable.ic_de_boca)
                        icon?.mutate()?.setTint(colorPrimary)
                        // IMPORTANTE: Definir o ícone no lado ESQUERDO
                        textView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
                        textView.compoundDrawablePadding = 0
                        
                        root.post {
                            if (root.width > 0) {
                                val textWidth = textView.paint.measureText(textView.text.toString())
                                val iconWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics).toInt()
                                val totalWidth = textWidth + iconWidth + textView.compoundDrawablePadding
                                textView.setPadding(((root.width - totalWidth) / 2).toInt(), 0, 0, 0)
                            }
                        }
                    }
                    else -> {
                        textView.text = score?.toString() ?: ""
                        textView.textSize = 45f
                    }
                }
            } else {
                // ESTADO NÃO PONTUADO (DISPONÍVEL)
                textView.text = categoryName
                textView.textSize = 20f
                textView.setTextColor(colorPrimary)
                textView.alpha = 0.5f
                textView.setTypeface(null, Typeface.BOLD)
                root.setBackgroundResource(R.drawable.background_card_black)
                root.backgroundTintList = ColorStateList.valueOf(colorWhite)
            }
        }
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryMarkerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            applyCategoryStyle(
                binding.categoryName,
                binding.root,
                category.name,
                category.score,
                category.isScored,
                category.isScratch,
                category.isBoca,
                binding.root.context
            )
            itemView.setOnClickListener { onCategoryClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryMarkerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size
}
