package com.example.infusoryapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.infusoryapp.databinding.ItemModelSelectionBinding
import com.example.infusoryapp.model.ModelItem

class ModelSelectionAdapter(
    private val items: List<ModelItem>,
    private val onSelected: (ModelItem) -> Unit
) : RecyclerView.Adapter<ModelSelectionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemModelSelectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemModelSelectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.name
        holder.binding.tvDescription.text = item.description
        holder.binding.root.setOnClickListener {
            onSelected(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
