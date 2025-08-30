package com.ruhidjavadoff.pichilti.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.ruhidjavadoff.pichilti.R
import com.ruhidjavadoff.pichilti.Room
import com.ruhidjavadoff.pichilti.databinding.FragmentCreateRoomBinding
import java.util.UUID

class CreateRoomFragment : Fragment() {

    private var _binding: FragmentCreateRoomBinding? = null
    private val binding get() = _binding!!
    private val database = Firebase.database("https://pichilti-chat-default-rtdb.europe-west1.firebasedatabase.app/").reference

    // Redaktə rejimində olduğumuzu və hansı otağı redaktə etdiyimizi bilmək üçün
    private var roomToEdit: Room? = null
    private var selectedAnimationPath: String? = null

    companion object {
        private const val ARG_ROOM_TO_EDIT = "room_to_edit"

        /**
         * Fragmenti həm boş (yaratmaq üçün), həm də otaq məlumatı ilə (redaktə üçün)
         * yaratmağa imkan verən yeni, təhlükəsiz üsul.
         */
        fun newInstance(room: Room? = null): CreateRoomFragment {
            val fragment = CreateRoomFragment()
            val args = Bundle()
            // Əgər redaktə üçün otaq göndərilibsə, onu arqumentlərə əlavə edirik
            room?.let {
                args.putParcelable(ARG_ROOM_TO_EDIT, it)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Arqumentlərdən redaktə ediləcək otağı alırıq
        arguments?.let {
            roomToEdit = it.getParcelable(ARG_ROOM_TO_EDIT)
        }
        setupResultListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Əgər redaktə rejimindəyiksə, səhifəni mövcud məlumatlarla doldururuq
        roomToEdit?.let { room ->
            populateUIForEdit(room)
        }

        binding.roomImageView.setOnClickListener { openImageSelection() }
        binding.chooseImageLabel.setOnClickListener { openImageSelection() }

        binding.createButton.setOnClickListener {
            saveRoom()
        }
    }

    /**
     * Redaktə rejimi üçün dizaynı mövcud otağın məlumatları ilə doldurur.
     */
    private fun populateUIForEdit(room: Room) {
        binding.titleTextView.text = "Edit Room"
        binding.roomNameEditText.setText(room.name)
        binding.roomDescriptionEditText.setText(room.description)
        binding.createButton.text = "Save Changes"

        // Mövcud animasiyanı göstəririk
        if (room.animationAsset.isNotEmpty()) {
            selectedAnimationPath = room.animationAsset
            LottieCompositionFactory.fromAsset(requireContext(), room.animationAsset).addListener { composition ->
                val lottieDrawable = LottieDrawable()
                lottieDrawable.composition = composition
                lottieDrawable.repeatCount = LottieDrawable.INFINITE
                binding.roomImageView.setImageDrawable(lottieDrawable)
                lottieDrawable.playAnimation()
            }
        }
    }

    /**
     * "Save" düyməsinə basıldıqda, redaktə və ya yaratma rejiminə uyğun funksiyanı çağırır.
     */
    private fun saveRoom() {
        val roomName = binding.roomNameEditText.text.toString().trim()
        val roomDescription = binding.roomDescriptionEditText.text.toString().trim()

        if (roomName.isEmpty() || roomDescription.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedAnimationPath == null) {
            Toast.makeText(context, "Please choose an image for the room", Toast.LENGTH_SHORT).show()
            return
        }

        // Redaktə rejimindəyiksə, məlumatları yeniləyirik, deyilsə yenisini yaradırıq
        if (roomToEdit != null) {
            updateRoomInFirebase(roomName, roomDescription)
        } else {
            createNewRoomInFirebase(roomName, roomDescription)
        }
    }

    private fun createNewRoomInFirebase(name: String, description: String) {
        val roomId = database.child("rooms").push().key ?: UUID.randomUUID().toString()
        val newRoom = Room(
            id = roomId,
            name = name,
            description = description,
            animationAsset = selectedAnimationPath!!,
            participantCount = 1
        )
        database.child("rooms").child(roomId).setValue(newRoom)
            .addOnSuccessListener {
                Toast.makeText(context, "Room created successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to create room: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CreateRoomFragment", "Firebase Error", e)
            }
    }

    private fun updateRoomInFirebase(name: String, description: String) {
        val roomToUpdate = roomToEdit!!
        val updatedRoom = roomToUpdate.copy(
            name = name,
            description = description,
            animationAsset = selectedAnimationPath!!
        )

        database.child("rooms").child(roomToUpdate.id).setValue(updatedRoom)
            .addOnSuccessListener {
                Toast.makeText(context, "Room updated successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update room: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CreateRoomFragment", "Firebase Error", e)
            }
    }

    private fun setupResultListener() {
        childFragmentManager.setFragmentResultListener(ImageSelectionFragment.REQUEST_KEY, this) { _, bundle ->
            val assetPath = bundle.getString(ImageSelectionFragment.KEY_ANIMATION_ASSET_PATH)
            if (assetPath != null) {
                selectedAnimationPath = assetPath
                LottieCompositionFactory.fromAsset(requireContext(), assetPath).addListener { composition ->
                    val lottieDrawable = LottieDrawable()
                    lottieDrawable.composition = composition
                    lottieDrawable.repeatCount = LottieDrawable.INFINITE
                    binding.roomImageView.setImageDrawable(lottieDrawable)
                    lottieDrawable.playAnimation()
                }
                binding.createRoomContentGroup.isVisible = true
                binding.imageSelectionFragmentContainer.isVisible = false
            }
        }
    }

    private fun openImageSelection() {
        binding.createRoomContentGroup.isVisible = false
        binding.imageSelectionFragmentContainer.isVisible = true
        childFragmentManager.beginTransaction()
            .replace(R.id.imageSelectionFragmentContainer, ImageSelectionFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}