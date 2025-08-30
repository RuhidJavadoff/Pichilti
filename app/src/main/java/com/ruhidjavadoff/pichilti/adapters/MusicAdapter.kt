package com.ruhidjavadoff.pichilti.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ruhidjavadoff.pichilti.MusicTrack
import com.ruhidjavadoff.pichilti.R
import com.ruhidjavadoff.pichilti.databinding.ItemMusicTrackBinding

class MusicAdapter(
    private var tracks: List<MusicTrack>,
    private val listener: OnTrackInteractionListener
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    // Hansı musiqinin hazırda oxuduğunu və onun vəziyyətini yadda saxlayırıq
    var currentlyPlayingId: String? = null
    var isPlaying: Boolean = false

    // Fragment ilə əlaqə üçün interfeys
    interface OnTrackInteractionListener {
        fun onPlayPauseClicked(track: MusicTrack)
        fun onTrackSelected(track: MusicTrack)
    }

    inner class MusicViewHolder(private val binding: ItemMusicTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(track: MusicTrack) {
            binding.trackTitleTextView.text = track.title
            binding.artistNameTextView.text = track.artist
            Glide.with(itemView.context).load(track.imageUrl).into(binding.albumArtImageView)

            // Musiqinin vəziyyətinə görə play/pause ikonunu təyin edirik
            if (track.id == currentlyPlayingId && isPlaying) {
                binding.playPauseButton.setImageResource(R.drawable.ic_pause)
            } else {
                binding.playPauseButton.setImageResource(R.drawable.ic_play_arrow)
            }

            // Play/Pause düyməsinə klikləndikdə
            binding.playPauseButton.setOnClickListener {
                listener.onPlayPauseClicked(track)
            }

            // Bütün elementə (göndərmək üçün) klikləndikdə
            binding.textContainer.setOnClickListener {
                listener.onTrackSelected(track)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding =
            ItemMusicTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.bind(tracks[position])
    }

    override fun getItemCount(): Int = tracks.size

    // Axtarış üçün siyahını yeniləyən funksiya
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(filteredTracks: List<MusicTrack>) {
        tracks = filteredTracks
        notifyDataSetChanged()
    }
}
