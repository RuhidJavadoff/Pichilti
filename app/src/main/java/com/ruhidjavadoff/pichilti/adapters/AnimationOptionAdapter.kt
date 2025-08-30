package com.ruhidjavadoff.pichilti.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ruhidjavadoff.pichilti.databinding.ItemAnimationOptionBinding

class AnimationOptionAdapter(
    private val assetFileNames: List<String>, // Artıq Int yox, String qəbul edir
    private val onAnimationSelected: (String) -> Unit
) : RecyclerView.Adapter<AnimationOptionAdapter.AnimationViewHolder>() {

    inner class AnimationViewHolder(val binding: ItemAnimationOptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(assetName: String) {
            // Animasiyanı assets qovluğundan adı ilə yükləyirik
            binding.animationView.setAnimation("room_animations/$assetName")
            binding.animationView.playAnimation()
            itemView.setOnClickListener {
                onAnimationSelected(assetName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimationViewHolder {
        val binding = ItemAnimationOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimationViewHolder, position: Int) {
        holder.bind(assetFileNames[position])
    }

    override fun getItemCount(): Int = assetFileNames.size
}