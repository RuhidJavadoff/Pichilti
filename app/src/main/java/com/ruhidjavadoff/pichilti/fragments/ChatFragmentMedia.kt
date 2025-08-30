package com.ruhidjavadoff.pichilti.fragments

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.ruhidjavadoff.pichilti.*
import com.ruhidjavadoff.pichilti.managers.FileUploadManager
import com.ruhidjavadoff.pichilti.services.MediaPlayerService
import java.util.*

// Bu fayl ChatFragment-in media və digər fragmentlərdən gələn nəticələrlə
// bağlı məntiqini saxlayır.

internal fun ChatFragment.setupServiceObservers() {
    mediaPlayerService?.let { service ->
        service.currentlyPlayingMessage.observe(viewLifecycleOwner) { message ->
            val currentState = service.playerState.value ?: MediaPlayerService.PlayerState.IDLE
            chatAdapter?.updatePlayerState(message?.messageId, currentState)
        }

        service.playerState.observe(viewLifecycleOwner) { state ->
            val currentMessageId = service.currentlyPlayingMessage.value?.messageId
            chatAdapter?.updatePlayerState(currentMessageId, state)
        }
    }
}

internal fun ChatFragment.setupResultListeners() {
    // "Ataç" menyusundan gələn nəticələri dinləyir
    childFragmentManager.setFragmentResultListener(AttachmentMenuFragment.REQUEST_KEY, this) { _, bundle ->
        val mainActivity = (activity as? MainActivity)
        when (bundle.getString(AttachmentMenuFragment.KEY_SELECTION)) {
            "music" -> mainActivity?.replaceFragment(MusicFragment(), true)
            "file" -> mainActivity?.replaceFragment(FilesFragment(), true)
            // DÜZƏLİŞ: MediaPickerFragment-i açarkən hansı növü istədiyimizi bildiririk
            "image" -> mainActivity?.replaceFragment(MediaPickerFragment.newInstance(MediaType.IMAGE), true)
            "video" -> mainActivity?.replaceFragment(MediaPickerFragment.newInstance(MediaType.VIDEO), true)
            else -> Toast.makeText(context, "Tezliklə", Toast.LENGTH_SHORT).show()
        }
    }

    // YENİ: MediaPickerFragment-dən gələn nəticəni dinləyirik
    parentFragmentManager.setFragmentResultListener(MediaPickerFragment.REQUEST_KEY, this) { _, bundle ->
        val selectedMedia: MediaItem? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(MediaPickerFragment.KEY_SELECTED_MEDIA, MediaItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(MediaPickerFragment.KEY_SELECTED_MEDIA)
        }

        selectedMedia?.let { mediaItem ->
            when (mediaItem.type) {
                MediaType.IMAGE -> uploadImageFile(mediaItem.uri)
                MediaType.VIDEO -> uploadVideoFile(mediaItem.uri)
            }
        }
    }

    // Musiqi seçim səhifəsindən gələn nəticəni dinləyir
    parentFragmentManager.setFragmentResultListener(MusicFragment.REQUEST_KEY, this) { _, bundle ->
        val track: MusicTrack? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(MusicFragment.KEY_SELECTED_TRACK, MusicTrack::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(MusicFragment.KEY_SELECTED_TRACK)
        }
        track?.let {
            val tempMessage = Message(
                messageId = UUID.randomUUID().toString(),
                senderId = currentUsername,
                senderName = currentUsername,
                timestamp = System.currentTimeMillis(),
                type = MessageType.MUSIC.name,
                musicTrack = it.apply {
                    status = TrackStatus.UPLOADING
                    localUri = it.trackUrl
                    trackUrl = ""
                }
            )
            messageList.add(tempMessage)
            chatAdapter?.notifyItemInserted(messageList.size - 1)
            binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
            uploadMusicFile(tempMessage)
        }
    }

    // Fayl seçim səhifəsindən gələn nəticəni dinləyir
    parentFragmentManager.setFragmentResultListener(FilesFragment.REQUEST_KEY, this) { _, bundle ->
        val fileModel: FileModel? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(FilesFragment.KEY_SELECTED_FILE, FileModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(FilesFragment.KEY_SELECTED_FILE)
        }

        fileModel?.let {
            val tempMessage = Message(
                messageId = UUID.randomUUID().toString(),
                senderId = currentUsername,
                senderName = currentUsername,
                timestamp = System.currentTimeMillis(),
                type = MessageType.FILE.name,
                fileMessage = FileMessage(
                    fileName = it.name,
                    fileSize = it.size,
                    localUri = it.uri.toString(),
                    status = TrackStatus.UPLOADING
                )
            )
            messageList.add(tempMessage)
            chatAdapter?.notifyItemInserted(messageList.size - 1)
            binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
            uploadGeneralFile(tempMessage)
        }
    }
}

internal fun ChatFragment.uploadMusicFile(message: Message) {
    val fileUri = Uri.parse(message.musicTrack!!.localUri)

    FileUploadManager.uploadMusicFile(message, fileUri, object : FileUploadManager.UploadCallback {
        override fun onProgress(progress: Int) {
            activity?.runOnUiThread {
                val index = messageList.indexOfFirst { it.messageId == message.messageId }
                if (index != -1) {
                    messageList[index].musicTrack?.progress = progress
                    chatAdapter?.notifyItemChanged(index, "PROGRESS_UPDATE")
                }
            }
        }

        override fun onSuccess(downloadUrl: Uri) {
            activity?.runOnUiThread {
                val tempId = message.messageId
                val newMessageId = messagesRef.push().key!!

                val finalMessageForFirebase = message.copy(
                    messageId = newMessageId,
                    musicTrack = message.musicTrack?.copy(
                        trackUrl = downloadUrl.toString(),
                        status = TrackStatus.IDLE,
                        progress = 0,
                        localUri = ""
                    )
                )

                messagesRef.child(newMessageId).setValue(finalMessageForFirebase)

                val index = messageList.indexOfFirst { it.messageId == tempId }
                if (index != -1) {
                    messageList[index] = message.copy(
                        messageId = newMessageId,
                        musicTrack = message.musicTrack?.copy(
                            trackUrl = downloadUrl.toString(),
                            status = TrackStatus.READY,
                            progress = 0
                        )
                    )
                    chatAdapter?.notifyItemChanged(index)
                }
            }
        }

        override fun onFailure(exception: Exception) {
            activity?.runOnUiThread {
                val index = messageList.indexOfFirst { it.messageId == message.messageId }
                if (index != -1) {
                    messageList[index].musicTrack?.status = TrackStatus.FAILED
                    chatAdapter?.notifyItemChanged(index)
                }
            }
        }
    })
}

internal fun ChatFragment.uploadGeneralFile(message: Message) {
    val fileUri = Uri.parse(message.fileMessage!!.localUri)

    FileUploadManager.uploadGeneralFile(message, fileUri, object : FileUploadManager.UploadCallback {
        override fun onProgress(progress: Int) {
            activity?.runOnUiThread {
                val index = messageList.indexOfFirst { it.messageId == message.messageId }
                if (index != -1) {
                    messageList[index].fileMessage?.progress = progress
                    chatAdapter?.notifyItemChanged(index, "PROGRESS_UPDATE")
                }
            }
        }

        override fun onSuccess(downloadUrl: Uri) {
            activity?.runOnUiThread {
                val tempId = message.messageId
                val newMessageId = messagesRef.push().key!!

                val finalMessageForFirebase = message.copy(
                    messageId = newMessageId,
                    fileMessage = message.fileMessage?.copy(
                        fileUrl = downloadUrl.toString(),
                        status = TrackStatus.IDLE,
                        progress = 0,
                        localUri = ""
                    )
                )

                messagesRef.child(newMessageId).setValue(finalMessageForFirebase)

                val index = messageList.indexOfFirst { it.messageId == tempId }
                if (index != -1) {
                    messageList[index] = message.copy(
                        messageId = newMessageId,
                        fileMessage = message.fileMessage?.copy(
                            fileUrl = downloadUrl.toString(),
                            status = TrackStatus.READY,
                            progress = 0
                        )
                    )
                    chatAdapter?.notifyItemChanged(index)
                }
            }
        }

        override fun onFailure(exception: Exception) {
            activity?.runOnUiThread {
                val index = messageList.indexOfFirst { it.messageId == message.messageId }
                if (index != -1) {
                    messageList[index].fileMessage?.status = TrackStatus.FAILED
                    chatAdapter?.notifyItemChanged(index)
                }
            }
        }
    })
}

internal fun ChatFragment.getAudioDuration(filePath: String): Long {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        durationStr?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        0L
    }
}
