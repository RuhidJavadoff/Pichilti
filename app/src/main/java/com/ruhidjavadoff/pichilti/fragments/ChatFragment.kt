package com.ruhidjavadoff.pichilti.fragments

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.ruhidjavadoff.pichilti.*
import com.ruhidjavadoff.pichilti.adapters.ChatAdapter
import com.ruhidjavadoff.pichilti.databinding.FragmentChatBinding
import com.ruhidjavadoff.pichilti.databinding.DialogReactionsBinding
import com.ruhidjavadoff.pichilti.managers.FileUploadManager
import com.ruhidjavadoff.pichilti.services.MediaPlayerService
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class ChatFragment : Fragment(), ChatAdapter.OnMediaInteractionListener {

    internal var _binding: FragmentChatBinding? = null
    internal val binding get() = _binding!!

    internal lateinit var room: Room
    internal lateinit var currentUsername: String
    internal val messageList = mutableListOf<Message>()
    internal var chatAdapter: ChatAdapter? = null

    internal var messageToReply: Message? = null

    internal lateinit var voiceRecorder: VoiceRecorder
    internal val playbackSpeeds = listOf(1.0f, 1.5f, 2.0f)
    internal var currentSpeedIndex = 0

    internal var isRecording = false
    internal val recordingHandler = Handler(Looper.getMainLooper())
    internal var recordingStartTime = 0L
    internal var recordingTimerRunnable: Runnable? = null

    internal lateinit var requestAudioPermissionLauncher: ActivityResultLauncher<String>

    internal val db = Firebase.database("https://pichilti-chat-default-rtdb.europe-west1.firebasedatabase.app/")
    internal val messagesRef: DatabaseReference by lazy { db.reference.child("messages").child(room.id) }
    internal val participantsRef: DatabaseReference by lazy { db.reference.child("room_participants").child(room.id) }
    internal val roomRef: DatabaseReference by lazy { db.reference.child("rooms").child(room.id) }


    internal lateinit var chatListener: ChildEventListener
    internal lateinit var participantListener: ValueEventListener

    internal var mediaPlayerService: MediaPlayerService? = null
    internal var isServiceBound = false
    internal val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MediaPlayerService.LocalBinder
            mediaPlayerService = binder.getService()
            isServiceBound = true
            setupServiceObservers()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            isServiceBound = false
        }
    }

    companion object {
        private const val ARG_ROOM = "arg_room"
        fun newInstance(room: Room): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ROOM, room)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { room = it.getParcelable(ARG_ROOM)!! }
        voiceRecorder = VoiceRecorder(requireContext())

        requestAudioPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) Toast.makeText(context, "İcazə verildi. Yenidən cəhd edin.", Toast.LENGTH_SHORT).show()
                else Toast.makeText(context, "Səsli mesaj göndərmək üçün icazə verməlisiniz.", Toast.LENGTH_LONG).show()
            }

        setupResultListeners()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsHeight = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, imeHeight + systemBarsHeight)
            windowInsets
        }
        currentUsername = (activity as? MainActivity)?.currentUsername ?: "Guest"

        setupUI()
        setupFirebaseListeners()
        setupSelectionHeaderListeners()
    }

    override fun onStart() {
        super.onStart()
        Intent(activity, MediaPlayerService::class.java).also { intent ->
            activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            activity?.unbindService(connection)
            isServiceBound = false
        }
        if (isRecording) {
            stopRecording(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeFirebaseListeners()
        _binding = null
    }

    // --- INTERFACE IMPLEMENTATIONS ---

    override fun onPlayPauseClicked(message: Message) { if (chatAdapter?.isSelectionMode == false) mediaPlayerService?.playOrPause(message) }
    override fun onDownloadClicked(message: Message) { if (chatAdapter?.isSelectionMode == false) downloadMedia(message) }
    override fun onCancelClicked(message: Message) { if (chatAdapter?.isSelectionMode == false) cancelMediaTransfer(message) }
    override fun onResendClicked(message: Message) { /* TODO */ }
    override fun onSpeedClicked(message: Message) { if (chatAdapter?.isSelectionMode == false) changePlaybackSpeed(message) }
    override fun onFileClicked(message: Message) { if (chatAdapter?.isSelectionMode == false) openSelectedFile(message) }
    override fun onImageClicked(message: Message) { if (chatAdapter?.isSelectionMode == false) openImageInFullScreen(message) }
    override fun onVideoClicked(message: Message) { if (chatAdapter?.isSelectionMode == false) playVideo(message) }

    override fun onMessageSelected(message: Message) = updateSelectionHeader()
    override fun onMessageDeselected(message: Message) = updateSelectionHeader()
    override fun onSelectionModeStarted() {
        binding.chatHeader.root.isVisible = false
        binding.chatHeaderSelectionMode.root.isVisible = true
        updateSelectionHeader()
    }

    // --- SELECTION MODE & REACTIONS LOGIC ---

    private fun setupSelectionHeaderListeners() {
        binding.chatHeaderSelectionMode.closeSelectionButton.setOnClickListener {
            chatAdapter?.clearSelection()
            exitSelectionMode()
        }
        binding.chatHeaderSelectionMode.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        binding.chatHeaderSelectionMode.reportButton.setOnClickListener {
            Toast.makeText(context, "Report feature coming soon.", Toast.LENGTH_SHORT).show()
        }
        binding.chatHeaderSelectionMode.reactButton.setOnClickListener {
            chatAdapter?.selectedMessages?.firstOrNull()?.let { message ->
                showReactionsDialog(message)
            }
        }
    }

    private fun updateSelectionHeader() {
        val selectedCount = chatAdapter?.selectedMessages?.size ?: 0
        binding.chatHeaderSelectionMode.selectionCountTextView.text = selectedCount.toString()
        binding.chatHeaderSelectionMode.reactButton.isVisible = selectedCount == 1

        if (selectedCount == 0) {
            exitSelectionMode()
        }
    }

    private fun exitSelectionMode() {
        binding.chatHeader.root.isVisible = true
        binding.chatHeaderSelectionMode.root.isVisible = false
        chatAdapter?.clearSelection()
    }

    private fun showDeleteConfirmationDialog() {
        val selected = chatAdapter?.selectedMessages ?: return
        if (selected.isEmpty()) return

        val canDeleteForEveryone = selected.all { it.senderId == currentUsername }
        val options = if (canDeleteForEveryone) {
            arrayOf("Delete for everyone", "Delete for me", "Cancel")
        } else {
            arrayOf("Delete for me", "Cancel")
        }

        AlertDialog.Builder(requireContext())
            .setItems(options) { dialog, which ->
                when (options[which]) {
                    "Delete for everyone" -> deleteMessagesForEveryone(selected.toList())
                    "Delete for me" -> deleteMessagesForMe(selected.toList())
                    "Cancel" -> dialog.dismiss()
                }
            }.show()
    }

    private fun deleteMessagesForEveryone(messagesToDelete: List<Message>) {
        messagesToDelete.forEach { message ->
            messagesRef.child(message.messageId).removeValue()
        }
        exitSelectionMode()
    }

    private fun deleteMessagesForMe(messagesToDelete: List<Message>) {
        deleteMessagesForEveryone(messagesToDelete)
    }

    private fun showReactionsDialog(message: Message) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogReactionsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val reactions = listOf("🫰�", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾")
        val reactionButtons = listOf(
            dialogBinding.reaction1, dialogBinding.reaction2, dialogBinding.reaction3,
            dialogBinding.reaction4, dialogBinding.reaction5, dialogBinding.reaction6,
            dialogBinding.reaction7, dialogBinding.reaction8, dialogBinding.reaction9,
            dialogBinding.reaction10
        )

        reactionButtons.forEachIndexed { index, textView ->
            textView.text = reactions[index]
            textView.setOnClickListener {
                reactToMessage(message, reactions[index])
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun reactToMessage(message: Message, emoji: String) {
        val currentReaction = message.reactions[currentUsername]
        val updates = mutableMapOf<String, Any?>()

        if (currentReaction == emoji) {
            updates["/reactions/$currentUsername"] = null
        } else {
            updates["/reactions/$currentUsername"] = emoji
        }

        messagesRef.child(message.messageId).updateChildren(updates)
        exitSelectionMode()
    }


    // --- HELPER FUNCTIONS ---
    private fun downloadMedia(message: Message) {
        when (MessageType.valueOf(message.type)) {
            MessageType.MUSIC -> downloadMusic(message)
            MessageType.FILE -> downloadFile(message)
            MessageType.IMAGE -> downloadImage(message)
            MessageType.VIDEO -> downloadVideo(message)
            else -> {}
        }
    }

    private fun cancelMediaTransfer(message: Message) {
        FileUploadManager.cancelDownload(message.messageId)
        FileUploadManager.cancelUpload(message.messageId)
        val index = messageList.indexOfFirst { it.messageId == message.messageId }
        if (index != -1) {
            val msg = messageList[index]
            msg.musicTrack?.status = TrackStatus.IDLE
            msg.fileMessage?.status = TrackStatus.IDLE
            msg.imageMessage?.status = TrackStatus.IDLE
            msg.videoMessage?.status = TrackStatus.IDLE
            chatAdapter?.notifyItemChanged(index)
        }
    }

    private fun changePlaybackSpeed(message: Message) {
        currentSpeedIndex = (currentSpeedIndex + 1) % playbackSpeeds.size
        val newSpeed = playbackSpeeds[currentSpeedIndex]
        mediaPlayerService?.setPlaybackSpeed(newSpeed)
        chatAdapter?.updatePlaybackSpeed(newSpeed)
    }

    private fun openSelectedFile(message: Message) {
        val fileMessage = message.fileMessage ?: return
        if (fileMessage.localUri.isEmpty()) {
            Toast.makeText(context, "Fayl hələ endirilməyib.", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(Uri.parse(fileMessage.localUri).path!!)
        val contentUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        openFile(contentUri, fileMessage.fileName)
    }

    private fun openImageInFullScreen(message: Message) {
        val imageMessage = message.imageMessage ?: return
        val imageUriString = if (imageMessage.localUri.isNotEmpty()) imageMessage.localUri else imageMessage.imageUrl
        if (imageUriString.isNotEmpty()) {
            (activity as? MainActivity)?.replaceFragment(ImageViewerFragment.newInstance(Uri.parse(imageUriString)), true)
        }
    }

    private fun playVideo(message: Message) {
        val videoMessage = message.videoMessage ?: return
        val videoUriString = if (videoMessage.localUri.isNotEmpty()) videoMessage.localUri else videoMessage.videoUrl
        if (videoUriString.isNotEmpty()) {
            try {
                val videoUri = Uri.parse(videoUriString)
                val intent = Intent(Intent.ACTION_VIEW, videoUri).apply {
                    setDataAndType(videoUri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Videonu aça biləcək tətbiq tapılmadı.", Toast.LENGTH_LONG).show()
            }
        }
    }

    internal fun downloadImage(message: Message) {
        val index = messageList.indexOfFirst { it.messageId == message.messageId }
        if (index == -1) return
        messageList[index].imageMessage?.status = TrackStatus.DOWNLOADING
        chatAdapter?.notifyItemChanged(index)
        FileUploadManager.downloadImageFile(requireContext(), message, object : FileUploadManager.DownloadCallback {
            override fun onProgress(progress: Int) {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].imageMessage?.progress = progress
                    chatAdapter?.notifyItemChanged(currentIndex, "PROGRESS_UPDATE")
                }
            }
            override fun onSuccess(localFileUri: Uri) {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].imageMessage?.apply {
                        localUri = localFileUri.toString()
                        status = TrackStatus.READY
                    }
                    chatAdapter?.notifyItemChanged(currentIndex)
                }
            }
            override fun onFailure(exception: Exception) {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].imageMessage?.status = TrackStatus.IDLE
                    chatAdapter?.notifyItemChanged(currentIndex)
                }
            }
        })
    }

    internal fun downloadVideo(message: Message) {
        val index = messageList.indexOfFirst { it.messageId == message.messageId }
        if (index == -1) return
        messageList[index].videoMessage?.status = TrackStatus.DOWNLOADING
        chatAdapter?.notifyItemChanged(index)
        FileUploadManager.downloadVideoFile(requireContext(), message, object : FileUploadManager.DownloadCallback {
            override fun onProgress(progress: Int) {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].videoMessage?.progress = progress
                    chatAdapter?.notifyItemChanged(currentIndex, "PROGRESS_UPDATE")
                }
            }
            override fun onSuccess(localFileUri: Uri) {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].videoMessage?.apply {
                        localUri = localFileUri.toString()
                        status = TrackStatus.READY
                    }
                    chatAdapter?.notifyItemChanged(currentIndex)
                }
            }
            override fun onFailure(exception: Exception) {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].videoMessage?.status = TrackStatus.IDLE
                    chatAdapter?.notifyItemChanged(currentIndex)
                }
            }
        })
    }
}
