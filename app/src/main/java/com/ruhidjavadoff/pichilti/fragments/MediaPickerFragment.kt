package com.ruhidjavadoff.pichilti.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.ruhidjavadoff.pichilti.MediaItem
import com.ruhidjavadoff.pichilti.MediaType
import com.ruhidjavadoff.pichilti.MediaRepository
import com.ruhidjavadoff.pichilti.adapters.MediaAdapter
import com.ruhidjavadoff.pichilti.databinding.FragmentMediaPickerBinding
import kotlinx.coroutines.launch

class MediaPickerFragment : Fragment() {

    private var _binding: FragmentMediaPickerBinding? = null
    private val binding get() = _binding!!

    private lateinit var mediaRepository: MediaRepository
    private lateinit var mediaAdapter: MediaAdapter
    private val mediaList = mutableListOf<MediaItem>()

    private var mediaTypeToShow: MediaType? = null

    companion object {
        const val REQUEST_KEY = "media_picker_request"
        const val KEY_SELECTED_MEDIA = "selected_media"
        private const val ARG_MEDIA_TYPE = "arg_media_type"

        fun newInstance(mediaType: MediaType): MediaPickerFragment {
            return MediaPickerFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_MEDIA_TYPE, mediaType)
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadMedia()
            } else {
                Toast.makeText(context, "Permission denied to read media.", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mediaTypeToShow = it.getSerializable(ARG_MEDIA_TYPE) as? MediaType
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mediaRepository = MediaRepository(requireContext())

        setupRecyclerView()
        checkPermissionAndLoadMedia()

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        mediaAdapter = MediaAdapter(mediaList) { selectedMedia ->
            setFragmentResult(REQUEST_KEY, bundleOf(KEY_SELECTED_MEDIA to selectedMedia))
            parentFragmentManager.popBackStack()
        }
        binding.mediaRecyclerView.adapter = mediaAdapter
    }

    private fun checkPermissionAndLoadMedia() {
        val permission = when(mediaTypeToShow) {
            MediaType.IMAGE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
            MediaType.VIDEO -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                loadMedia()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun loadMedia() {
        binding.progressBar.isVisible = true
        lifecycleScope.launch {
            val allMedia = mediaRepository.getAllMedia()

            val filteredMedia = if (mediaTypeToShow != null) {
                allMedia.filter { it.type == mediaTypeToShow }
            } else {
                allMedia
            }

            mediaList.clear()
            mediaList.addAll(filteredMedia)
            mediaAdapter.notifyDataSetChanged()
            binding.progressBar.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
