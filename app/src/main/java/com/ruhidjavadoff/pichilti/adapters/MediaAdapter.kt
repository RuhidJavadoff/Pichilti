package com.ruhidjavadoff.pichilti.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ruhidjavadoff.pichilti.MediaItem
import com.ruhidjavadoff.pichilti.MediaType
import com.ruhidjavadoff.pichilti.databinding.ItemMediaBinding

class MediaAdapter(
    private val mediaList: List<MediaItem>,
    private val onMediaSelected: (MediaItem) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(private val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mediaItem: MediaItem) {
            Glide.with(itemView.context)
                .load(mediaItem.uri)
                .centerCrop()
                .into(binding.mediaImageView)

            if (mediaItem.type == MediaType.VIDEO) {
                binding.durationTextView.isVisible = true
                binding.durationTextView.text = mediaItem.duration
            } else {
                binding.durationTextView.isVisible = false
            }

            itemView.setOnClickListener {
                onMediaSelected(mediaItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(mediaList[position])
    }

    override fun getItemCount(): Int = mediaList.size
}
