package com.ruhidjavadoff.pichilti.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ruhidjavadoff.pichilti.Room
import com.ruhidjavadoff.pichilti.databinding.ItemRoomBinding

// DƏYİŞİKLİK: Adapterə klikləmə funksiyası əlavə edirik
class RoomAdapter(
    private val rooms: List<Room>,
    private val onRoomClicked: (Room) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(private val binding: ItemRoomBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(room: Room) {
            binding.roomNameTextView.text = room.name
            binding.roomDescriptionTextView.text = room.description
            binding.participantCountTextView.text = room.participantCount.toString()

            if (room.animationAsset.isNotEmpty()) {
                binding.roomImageView.setAnimation(room.animationAsset)
                binding.roomImageView.playAnimation()
            } else {
                Glide.with(itemView.context)
                    .load(room.imageUrl)
                    .centerCrop()
                    .into(binding.roomImageView)
            }

            // DƏYİŞİKLİK: Bütün otaq elementinə klikləmə məntiqi əlavə edirik
            itemView.setOnClickListener {
                onRoomClicked(room)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(rooms[position])
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    fun getRoomAt(position: Int): Room {
        return rooms[position]
    }
}