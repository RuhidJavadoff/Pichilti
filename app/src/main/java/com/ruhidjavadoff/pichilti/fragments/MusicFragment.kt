package com.ruhidjavadoff.pichilti.fragments

import android.Manifest
import android.content.ContentUris
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.ruhidjavadoff.pichilti.MusicTrack
import com.ruhidjavadoff.pichilti.adapters.MusicAdapter
import com.ruhidjavadoff.pichilti.databinding.FragmentMusicBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicFragment : Fragment(), MusicAdapter.OnTrackInteractionListener {

    private var _binding: FragmentMusicBinding? = null
    private val binding get() = _binding!!

    private lateinit var musicAdapter: MusicAdapter
    private val allMusicTracks = mutableListOf<MusicTrack>()
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val REQUEST_KEY = "music_selection_request"
        const val KEY_SELECTED_TRACK = "selected_track"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadMusicFromDevice()
            } else {
                Toast.makeText(context, "Permission denied. Cannot load music.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMediaPlayer()
        setupRecyclerView()
        setupSearch()
        checkPermissionAndLoadMusic()
    }

    private fun checkPermissionAndLoadMusic() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestPermissionLauncher.launch(permission)
    }

    private fun loadMusicFromDevice() {
        lifecycleScope.launch(Dispatchers.IO) {
            val musicList = mutableListOf<MusicTrack>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION
            )
            val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
            val selectionArgs = arrayOf("60000")

            val cursor = requireContext().contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn)
                    val artist = it.getString(artistColumn)
                    val albumId = it.getLong(albumIdColumn)

                    val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

                    musicList.add(
                        MusicTrack(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            imageUrl = albumArtUri.toString(),
                            trackUrl = trackUri.toString()
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                allMusicTracks.clear()
                allMusicTracks.addAll(musicList)
                musicAdapter.updateList(allMusicTracks)
            }
        }
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicAdapter(allMusicTracks, this)
        binding.musicRecyclerView.adapter = musicAdapter
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filteredList = allMusicTracks.filter {
                    it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                }
                musicAdapter.updateList(filteredList)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onPlayPauseClicked(track: MusicTrack) {
        try {
            if (mediaPlayer?.isPlaying == true && musicAdapter.currentlyPlayingId == track.id) {
                mediaPlayer?.pause()
                musicAdapter.isPlaying = false
            } else {
                mediaPlayer?.reset()
                mediaPlayer?.setDataSource(requireContext(), Uri.parse(track.trackUrl))
                mediaPlayer?.prepareAsync()
                mediaPlayer?.setOnPreparedListener {
                    it.start()
                    musicAdapter.isPlaying = true
                }
                musicAdapter.currentlyPlayingId = track.id
            }
            musicAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot play this file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onTrackSelected(track: MusicTrack) {
        setFragmentResult(REQUEST_KEY, bundleOf(KEY_SELECTED_TRACK to track))
        parentFragmentManager.popBackStack()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
        if(::musicAdapter.isInitialized){
            musicAdapter.isPlaying = false
            musicAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }
}