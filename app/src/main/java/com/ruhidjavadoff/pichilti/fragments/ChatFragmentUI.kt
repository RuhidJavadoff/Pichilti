package com.ruhidjavadoff.pichilti.fragments

import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ruhidjavadoff.pichilti.*
import com.ruhidjavadoff.pichilti.adapters.ChatAdapter
import com.ruhidjavadoff.pichilti.managers.FileUploadManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

internal fun ChatFragment.setupUI() {
    setupRecyclerView()
    setupHeader()
    setupFooter()
}

private fun ChatFragment.setupRecyclerView() {
    chatAdapter = ChatAdapter(messageList, currentUsername, this)
    val layoutManager = LinearLayoutManager(context)
    layoutManager.stackFromEnd = true
    binding.chatRecyclerView.layoutManager = layoutManager
    binding.chatRecyclerView.adapter = chatAdapter

    val swipeToReplyCallback = object : SwipeToReplyCallback(requireContext()) {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val message = messageList[position]
            showReplyingTo(message)
            chatAdapter?.notifyItemChanged(position)
        }
    }

    val itemTouchHelper = ItemTouchHelper(swipeToReplyCallback)
    itemTouchHelper.attachToRecyclerView(binding.chatRecyclerView)
}

private fun ChatFragment.setupHeader() {
    binding.chatHeader.headerRoomName.text = room.name
    if (room.animationAsset.isNotEmpty()) {
        binding.chatHeader.headerRoomAvatar.setAnimation(room.animationAsset)
        binding.chatHeader.headerRoomAvatar.playAnimation()
    }
    binding.chatHeader.toolbar.setNavigationOnClickListener {
        parentFragmentManager.popBackStack()
    }
    binding.chatHeader.headerMenuButton.setOnClickListener {
        // Menu logic here
    }
}

private fun ChatFragment.setupFooter() {
    binding.chatFooter.messageEditText.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val isEmpty = s.toString().trim().isEmpty()
            binding.chatFooter.actionButton.setImageResource(
                if (isEmpty) R.drawable.ic_microphone else R.drawable.ic_send
            )
        }
    })

    binding.chatFooter.attachButton.setOnClickListener {
        val attachmentMenu = AttachmentMenuFragment()
        attachmentMenu.show(childFragmentManager, "AttachmentMenu")
    }

    binding.chatFooter.actionButton.setOnClickListener {
        if (isRecording) {
            stopRecording(true)
        } else {
            val messageText = binding.chatFooter.messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.chatFooter.messageEditText.text.clear()
            } else {
                startRecording()
            }
        }
    }

    binding.chatFooter.cancelRecordingButton.setOnClickListener {
        stopRecording(false)
    }

    binding.chatFooter.cancelReplyButton.setOnClickListener {
        clearReplyingTo()
    }
}

internal fun ChatFragment.showReplyingTo(message: Message) {
    messageToReply = message
    binding.chatFooter.replyingToLayout.isVisible = true
    val username = if (message.senderId == currentUsername) "You" else message.senderName
    binding.chatFooter.replyingToUsername.text = username

    val preview = getMessagePreview(message)
    binding.chatFooter.replyingToMessagePreview.text = preview
}

internal fun ChatFragment.clearReplyingTo() {
    messageToReply = null
    binding.chatFooter.replyingToLayout.isVisible = false
}

// --- BÜTÜN GÖNDƏRMƏ MƏNTİQİ BURADA BİRLƏŞDİRİLDİ ---

private fun ChatFragment.createReplyInfo(): ReplyInfo? {
    return messageToReply?.let {
        ReplyInfo(
            repliedToMessageId = it.messageId,
            repliedToSenderName = it.senderName,
            repliedToMessagePreview = getMessagePreview(it)
        )
    }
}

private fun getMessagePreview(message: Message): String {
    return when (MessageType.valueOf(message.type)) {
        MessageType.TEXT -> message.text ?: ""
        MessageType.IMAGE -> "Photo"
        MessageType.VIDEO -> "Video"
        MessageType.VOICE -> "Voice Message"
        MessageType.MUSIC -> "Music: ${message.musicTrack?.title}"
        MessageType.FILE -> "File: ${message.fileMessage?.fileName}"
    }
}

internal fun ChatFragment.sendMessage(text: String) {
    val replyInfo = createReplyInfo()
    val messageId = messagesRef.push().key!!
    val finalMessage = Message(
        messageId = messageId,
        text = text,
        senderId = currentUsername,
        senderName = currentUsername,
        timestamp = System.currentTimeMillis(),
        type = MessageType.TEXT.name,
        replyInfo = replyInfo
    )
    messagesRef.child(messageId).setValue(finalMessage)
    clearReplyingTo()
}

internal fun ChatFragment.sendVoiceMessage(filePath: String) {
    val replyInfo = createReplyInfo()
    val fileUri = File(filePath).toUri()
    val duration = getAudioDuration(filePath)

    val tempMessage = Message(
        messageId = UUID.randomUUID().toString(),
        senderId = currentUsername,
        senderName = currentUsername,
        timestamp = System.currentTimeMillis(),
        type = MessageType.VOICE.name,
        voiceMessage = VoiceMessage(duration = duration, localUri = fileUri.toString()),
        replyInfo = replyInfo
    )
    messageList.add(tempMessage)
    chatAdapter?.notifyItemInserted(messageList.size - 1)
    binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
    clearReplyingTo()

    FileUploadManager.uploadVoiceFile(tempMessage, fileUri, object : FileUploadManager.UploadCallback {
        override fun onProgress(progress: Int) {}
        override fun onSuccess(downloadUrl: Uri) {
            activity?.runOnUiThread {
                val newMessageId = messagesRef.push().key!!
                val finalMessage = tempMessage.copy(
                    messageId = newMessageId,
                    voiceMessage = tempMessage.voiceMessage?.copy(voiceUrl = downloadUrl.toString(), localUri = "")
                )
                messagesRef.child(newMessageId).setValue(finalMessage)
                val index = messageList.indexOfFirst { it.messageId == tempMessage.messageId }
                if (index != -1) {
                    messageList[index] = finalMessage.copy(
                        voiceMessage = finalMessage.voiceMessage?.copy(localUri = tempMessage.voiceMessage!!.localUri)
                    )
                    chatAdapter?.notifyItemChanged(index)
                }
            }
        }
        override fun onFailure(exception: Exception) {
            activity?.runOnUiThread {
                val index = messageList.indexOfFirst { it.messageId == tempMessage.messageId }
                if (index != -1) {
                    messageList.removeAt(index)
                    chatAdapter?.notifyItemRemoved(index)
                }
            }
        }
    })
}

internal fun ChatFragment.sendMediaMessage(mediaItem: MediaItem) {
    val replyInfo = createReplyInfo()
    val tempMessage: Message

    if (mediaItem.type == MediaType.IMAGE) {
        tempMessage = Message(
            messageId = UUID.randomUUID().toString(),
            senderId = currentUsername,
            senderName = currentUsername,
            timestamp = System.currentTimeMillis(),
            type = MessageType.IMAGE.name,
            imageMessage = ImageMessage(localUri = mediaItem.uri.toString()),
            replyInfo = replyInfo
        )
        messageList.add(tempMessage)
        chatAdapter?.notifyItemInserted(messageList.size - 1)
        binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
        clearReplyingTo()

        FileUploadManager.uploadImageFile(tempMessage, mediaItem.uri, object : FileUploadManager.UploadCallback {
            override fun onProgress(progress: Int) { /* ... */ }
            override fun onSuccess(downloadUrl: Uri) {
                val newMessageId = messagesRef.push().key!!
                val finalMessage = tempMessage.copy(
                    messageId = newMessageId,
                    imageMessage = tempMessage.imageMessage?.copy(imageUrl = downloadUrl.toString(), status = TrackStatus.IDLE, localUri = "")
                )
                messagesRef.child(newMessageId).setValue(finalMessage)
                val index = messageList.indexOfFirst { it.messageId == tempMessage.messageId }
                if (index != -1) {
                    messageList[index] = finalMessage.copy(
                        imageMessage = finalMessage.imageMessage?.copy(localUri = tempMessage.imageMessage!!.localUri, status = TrackStatus.READY)
                    )
                    chatAdapter?.notifyItemChanged(index)
                }
            }
            override fun onFailure(exception: Exception) { /* ... */ }
        })

    } else if (mediaItem.type == MediaType.VIDEO) {
        tempMessage = Message(
            messageId = UUID.randomUUID().toString(),
            senderId = currentUsername,
            senderName = currentUsername,
            timestamp = System.currentTimeMillis(),
            type = MessageType.VIDEO.name,
            videoMessage = VideoMessage(localUri = mediaItem.uri.toString(), status = TrackStatus.UPLOADING),
            replyInfo = replyInfo
        )
        messageList.add(tempMessage)
        chatAdapter?.notifyItemInserted(messageList.size - 1)
        binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
        clearReplyingTo()

        lifecycleScope.launch {
            FileUploadManager.uploadVideoFile(requireContext(), tempMessage, mediaItem.uri, object : FileUploadManager.VideoUploadCallback {
                override fun onProgress(progress: Int) { /* ... */ }
                override fun onSuccess(videoUrl: Uri, thumbnailUrl: Uri) {
                    val newMessageId = messagesRef.push().key!!
                    val finalMessage = tempMessage.copy(
                        messageId = newMessageId,
                        videoMessage = tempMessage.videoMessage?.copy(
                            videoUrl = videoUrl.toString(),
                            thumbnailUrl = thumbnailUrl.toString(),
                            status = TrackStatus.IDLE,
                            localUri = ""
                        )
                    )
                    messagesRef.child(newMessageId).setValue(finalMessage)
                    val index = messageList.indexOfFirst { it.messageId == tempMessage.messageId }
                    if (index != -1) {
                        messageList[index] = finalMessage.copy(
                            videoMessage = finalMessage.videoMessage?.copy(localUri = tempMessage.videoMessage!!.localUri, status = TrackStatus.READY)
                        )
                        chatAdapter?.notifyItemChanged(index)
                    }
                }
                override fun onFailure(exception: Exception) { /* ... */ }
            })
        }
    }
}

internal fun ChatFragment.updateRecordingUI(isRecordingActive: Boolean) {
    binding.voiceRecordAnimationView.isVisible = isRecordingActive
    binding.chatFooter.messageEditText.isVisible = !isRecordingActive
    binding.chatFooter.attachButton.isVisible = !isRecordingActive
    binding.chatFooter.voiceRecordingLayout.isVisible = isRecordingActive

    if (isRecordingActive) {
        binding.voiceRecordAnimationView.playAnimation()
        binding.chatFooter.actionButton.setImageResource(R.drawable.ic_send)
        val anim = AlphaAnimation(0.2f, 1.0f).apply {
            duration = 700
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        binding.chatFooter.recordingIndicator.startAnimation(anim)
    } else {
        binding.voiceRecordAnimationView.cancelAnimation()
        binding.chatFooter.actionButton.setImageResource(R.drawable.ic_microphone)
        binding.chatFooter.recordingIndicator.clearAnimation()
    }
}
