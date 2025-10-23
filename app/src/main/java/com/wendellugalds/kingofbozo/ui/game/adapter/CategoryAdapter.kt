package com.wendellugalds.kingofbozo.ui.game.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
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

    fun getCategoryAt(position: Int): Category? {
        return if (position >= 0 && position < categories.size) {
            categories[position]
        } else {
            null
        }
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryMarkerBinding) : RecyclerView.ViewHolder(binding.root) {

        // --- FUNÇÃO BIND CORRIGIDA PARA EVITAR BUG DE RECICLAGEM ---
        fun bind(category: Category) {
            val context = binding.root.context
            val categoryTextView = binding.categoryName

            // Reseta o clique para ser sempre ativo por padrão
            itemView.isClickable = true

            if (category.isScored) {
                // --- ESTADO PONTUADO ---
                binding.root.background = ContextCompat.getDrawable(context, R.drawable.background_category_item_collapsed)
                categoryTextView.setTypeface(null, Typeface.BOLD) // Usa negrito para pontuações
                categoryTextView.textSize = 70f // Tamanho maior para pontuações
                categoryTextView.setTextColor(ContextCompat.getColor(context, R.color.black))

                if (category.score == 0 && category.isScratch) {
                    // Se foi RISCADO, mostra "X"
                    categoryTextView.text = "X"
                    categoryTextView.textSize = 70f
                    categoryTextView.setTextColor(ContextCompat.getColor(context, R.color.black))
                    binding.root.background = ContextCompat.getDrawable(context, R.drawable.background_score_option_riscar)
                } else if (category.score == 0 && !category.isScratch) {
                    // Se foi ZERADO (Nulo), mostra o NOME
                    categoryTextView.text = category.name
                    categoryTextView.textSize = 17f // Tamanho normal
                    categoryTextView.setTypeface(null, Typeface.BOLD) // Nome da categoria em negrito
                    categoryTextView.setTextColor(ContextCompat.getColor(context, R.color.black))
                } else {
                    // Se tem uma pontuação, mostra o NÚMERO
                    categoryTextView.text = category.score.toString()
                }
            } else {
                // --- ESTADO NÃO PONTUADO ---
                categoryTextView.text = category.name
                categoryTextView.textSize = 17f // Garante o tamanho normal
                categoryTextView.setTypeface(null, Typeface.BOLD) // Nome da categoria em negrito
                categoryTextView.setTextColor(ContextCompat.getColor(context, R.color.black))
                binding.root.background = ContextCompat.getDrawable(context, R.drawable.background_category_item_collapsed)
            }

            itemView.setOnClickListener {
                onCategoryClick(category)
            }
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