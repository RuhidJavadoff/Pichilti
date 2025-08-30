package com.ruhidjavadoff.pichilti.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruhidjavadoff.pichilti.databinding.FragmentAttachmentMenuBinding

class AttachmentMenuFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAttachmentMenuBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val REQUEST_KEY = "attachment_request"
        const val KEY_SELECTION = "selected_option"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttachmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionImage.setOnClickListener {
            setResultAndDismiss("image")
        }
        binding.optionVideo.setOnClickListener {
            setResultAndDismiss("video")
        }
        binding.optionMusic.setOnClickListener {
            // Əsas məqsədimiz budur
            setResultAndDismiss("music")
        }
        binding.optionFile.setOnClickListener {
            setResultAndDismiss("file")
        }
    }

    private fun setResultAndDismiss(selection: String) {
        // Seçimi bir "bağlama"ya (Bundle) qoyub, nəticəni geri göndəririk
        setFragmentResult(REQUEST_KEY, bundleOf(KEY_SELECTION to selection))
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}