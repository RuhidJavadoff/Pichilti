package com.ruhidjavadoff.pichilti.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.ruhidjavadoff.pichilti.adapters.AnimationOptionAdapter
import com.ruhidjavadoff.pichilti.databinding.FragmentImageSelectionBinding
import java.io.IOException

class ImageSelectionFragment : Fragment() {

    private var _binding: FragmentImageSelectionBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val REQUEST_KEY = "image_selection_request"
        const val KEY_ANIMATION_ASSET_PATH = "animation_asset_path" // Artıq ID yox, fayl yolunu göndəririk
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.selectFromDeviceButton.setOnClickListener { /* Gələcəkdə əlavə ediləcək */ }
        setupAnimationsList()
    }

    private fun setupAnimationsList() {
        // assets/room_animations qovluğundakı bütün faylların adını alırıq
        val animationFileNames = try {
            requireContext().assets.list("room_animations")?.toList() ?: emptyList()
        } catch (e: IOException) {
            Log.e("ImageSelection", "Could not list assets", e)
            emptyList()
        }

        val adapter = AnimationOptionAdapter(animationFileNames) { selectedAssetName ->
            // Seçilmiş animasiyanın tam yolunu (path) nəticə olaraq göndəririk
            val assetPath = "room_animations/$selectedAssetName"
            val result = bundleOf(KEY_ANIMATION_ASSET_PATH to assetPath)
            setFragmentResult(REQUEST_KEY, result)
            parentFragmentManager.popBackStack()
        }
        binding.animationsRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}