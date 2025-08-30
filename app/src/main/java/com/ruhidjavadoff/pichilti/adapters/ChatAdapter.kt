package com.ruhidjavadoff.pichilti.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexboxLayout
import com.ruhidjavadoff.pichilti.Message
import com.ruhidjavadoff.pichilti.MessageType
import com.ruhidjavadoff.pichilti.R
import com.ruhidjavadoff.pichilti.TrackStatus
import com.ruhidjavadoff.pichilti.databinding.*
import com.ruhidjavadoff.pichilti.services.MediaPlayerService
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ChatAdapter(
    private val messages: List<Message>,
    private val currentUserId: String,
    private val listener: OnMediaInteractionListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isSelectionMode = false
    val selectedMessages = mutableSetOf<Message>()

    private var playingMessageId: String? = null
    private var playerState: MediaPlayerService.PlayerState = MediaPlayerService.PlayerState.IDLE
    private var currentSpeed: Float = 1.0f

    fun updatePlayerState(messageId: String?, state: MediaPlayerService.PlayerState) {
        val previousPlayingId = playingMessageId
        val currentPlayingId = messageId
        playingMessageId = messageId
        playerState = state
        if (state == MediaPlayerService.PlayerState.IDLE) {
            currentSpeed = 1.0f
        }
        if (previousPlayingId != null) {
            val index = messages.indexOfFirst { it.messageId == previousPlayingId }
            if (index != -1) notifyItemChanged(index)
        }
        if (currentPlayingId != null) {
            val index = messages.indexOfFirst { it.messageId == currentPlayingId }
            if (index != -1) notifyItemChanged(index)
        }
    }

    fun updatePlaybackSpeed(speed: Float) {
        currentSpeed = speed
        if (playingMessageId != null) {
            val index = messages.indexOfFirst { it.messageId == playingMessageId }
            if (index != -1) {
                notifyItemChanged(index)
            }
        }
    }

    interface OnMediaInteractionListener {
        fun onPlayPauseClicked(message: Message)
        fun onDownloadClicked(message: Message)
        fun onCancelClicked(message: Message)
        fun onResendClicked(message: Message)
        fun onSpeedClicked(message: Message)
        fun onFileClicked(message: Message)
        fun onImageClicked(message: Message)
        fun onVideoClicked(message: Message)
        fun onMessageSelected(message: Message)
        fun onMessageDeselected(message: Message)
        fun onSelectionModeStarted()
    }

    companion object {
        private const val VIEW_TYPE_TEXT_INCOMING = 1
        private const val VIEW_TYPE_TEXT_OUTGOING = 2
        private const val VIEW_TYPE_MUSIC_INCOMING = 3
        private const val VIEW_TYPE_MUSIC_OUTGOING = 4
        private const val VIEW_TYPE_VOICE_INCOMING = 5
        private const val VIEW_TYPE_VOICE_OUTGOING = 6
        private const val VIEW_TYPE_FILE_INCOMING = 7
        private const val VIEW_TYPE_FILE_OUTGOING = 8
        private const val VIEW_TYPE_IMAGE_INCOMING = 9
        private const val VIEW_TYPE_IMAGE_OUTGOING = 10
        private const val VIEW_TYPE_VIDEO_INCOMING = 11
        private const val VIEW_TYPE_VIDEO_OUTGOING = 12
    }

    fun startSelectionMode(position: Int) {
        if (position == RecyclerView.NO_POSITION) return
        isSelectionMode = true
        toggleSelection(messages[position])
        listener.onSelectionModeStarted()
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedMessages.clear()
        notifyDataSetChanged()
    }

    private fun toggleSelection(message: Message) {
        if (selectedMessages.contains(message)) {
            selectedMessages.remove(message)
            listener.onMessageDeselected(message)
        } else {
            selectedMessages.add(message)
            listener.onMessageSelected(message)
        }
        notifyItemChanged(messages.indexOf(message))
    }

    private fun handleSelection(itemView: View, message: Message, holder: RecyclerView.ViewHolder) {
        if (selectedMessages.contains(message)) {
            itemView.setBackgroundColor(Color.parseColor("#803498DB")) // Light blue selection color
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                startSelectionMode(holder.bindingAdapterPosition)
            }
            true
        }

        itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(message)
            }
        }
    }

    private fun handleReply(
        message: Message,
        replyLayout: View,
        replySenderTextView: TextView,
        replyPreviewTextView: TextView
    ) {
        if (message.replyInfo != null) {
            replyLayout.isVisible = true
            val senderText = if (message.replyInfo!!.repliedToSenderName == currentUserId) "You" else message.replyInfo!!.repliedToSenderName
            replySenderTextView.text = "Replied to $senderText"
            replyPreviewTextView.text = message.replyInfo!!.repliedToMessagePreview
        } else {
            replyLayout.isVisible = false
        }
    }

    private fun handleReactions(message: Message, reactionsLayout: FlexboxLayout) {
        reactionsLayout.removeAllViews()
        if (message.reactions.isNotEmpty()) {
            reactionsLayout.isVisible = true
            val reactionCounts = message.reactions.values.groupingBy { it }.eachCount()
            reactionCounts.forEach { (emoji, count) ->
                val reactionView = LayoutInflater.from(reactionsLayout.context)
                    .inflate(R.layout.item_reaction, reactionsLayout, false) as TextView
                reactionView.text = "$emoji $count"
                reactionsLayout.addView(reactionView)
            }
        } else {
            reactionsLayout.isVisible = false
        }
    }

    inner class IncomingTextViewHolder(val binding: ItemChatMessageIncomingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.senderNameTextView.text = message.senderName
            binding.messageTextView.text = message.text
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            handleSelection(itemView, message, this)
            handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class OutgoingTextViewHolder(val binding: ItemChatMessageOutgoingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.messageTextView.text = message.text
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            handleSelection(itemView, message, this)
            handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class IncomingMusicViewHolder(val binding: ItemChatMessageMusicIncomingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            val track = message.musicTrack!!
            binding.senderNameTextView.text = message.senderName
            binding.trackTitleTextView.text = track.title
            binding.artistNameTextView.text = track.artist
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            binding.mediaProgressBar.isVisible = false
            binding.mediaControlButton.isVisible = true
            val isThisTrackPlaying = playingMessageId == message.messageId && playerState == MediaPlayerService.PlayerState.PLAYING

            if (isThisTrackPlaying) {
                binding.mediaControlButton.setImageResource(R.drawable.ic_pause)
            } else {
                binding.mediaControlButton.setImageResource(R.drawable.ic_play_arrow)
            }
            binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onPlayPauseClicked(message) }

            when (track.status) {
                TrackStatus.DOWNLOADING -> {
                    binding.mediaProgressBar.progress = track.progress
                    binding.mediaProgressBar.isVisible = true
                    binding.mediaControlButton.setImageResource(R.drawable.ic_cancel)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onCancelClicked(message) }
                }
                TrackStatus.READY -> {}
                else -> {
                    binding.mediaControlButton.setImageResource(R.drawable.ic_download)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onDownloadClicked(message) }
                }
            }
            handleSelection(itemView, message, this)
            // handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class OutgoingMusicViewHolder(val binding: ItemChatMessageMusicOutgoingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            val track = message.musicTrack!!
            binding.trackTitleTextView.text = track.title
            binding.artistNameTextView.text = track.artist
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            binding.mediaProgressBar.isVisible = false
            binding.mediaControlButton.isVisible = true
            val isThisTrackPlaying = playingMessageId == message.messageId && playerState == MediaPlayerService.PlayerState.PLAYING

            if (isThisTrackPlaying) {
                binding.mediaControlButton.setImageResource(R.drawable.ic_pause)
            } else {
                binding.mediaControlButton.setImageResource(R.drawable.ic_play_arrow)
            }
            binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onPlayPauseClicked(message) }

            when (track.status) {
                TrackStatus.UPLOADING, TrackStatus.DOWNLOADING -> {
                    binding.mediaProgressBar.progress = track.progress
                    binding.mediaProgressBar.isVisible = true
                    binding.mediaControlButton.setImageResource(R.drawable.ic_cancel)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onCancelClicked(message) }
                }
                TrackStatus.FAILED -> {
                    binding.mediaControlButton.setImageResource(R.drawable.ic_send_media)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onResendClicked(message) }
                }
                else -> {}
            }
            handleSelection(itemView, message, this)
            // handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class IncomingVoiceViewHolder(val binding: ItemChatMessageVoiceIncomingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            binding.senderNameTextView.text = message.senderName
            binding.durationTextView.text = formatDuration(message.voiceMessage?.duration ?: 0L)
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            val isThisTrackPlaying = playingMessageId == message.messageId && playerState == MediaPlayerService.PlayerState.PLAYING

            if (isThisTrackPlaying) {
                binding.mediaControlButton.setImageResource(R.drawable.ic_pause)
                binding.waveformView.playAnimation()
                binding.speedTextView.isVisible = true
                binding.speedTextView.text = "${currentSpeed}x".replace(".0", "")
            } else {
                binding.mediaControlButton.setImageResource(R.drawable.ic_play_arrow)
                binding.waveformView.cancelAnimation()
                binding.waveformView.progress = 0f
                binding.speedTextView.isVisible = false
            }

            binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onPlayPauseClicked(message) }
            binding.speedTextView.setOnClickListener { if (!isSelectionMode) listener.onSpeedClicked(message) }
            handleSelection(itemView, message, this)
            // handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class OutgoingVoiceViewHolder(val binding: ItemChatMessageVoiceOutgoingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            binding.durationTextView.text = formatDuration(message.voiceMessage?.duration ?: 0L)
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            val isThisTrackPlaying = playingMessageId == message.messageId && playerState == MediaPlayerService.PlayerState.PLAYING

            if (isThisTrackPlaying) {
                binding.mediaControlButton.setImageResource(R.drawable.ic_pause)
                binding.waveformView.playAnimation()
                binding.speedTextView.isVisible = true
                binding.speedTextView.text = "${currentSpeed}x".replace(".0", "")
            } else {
                binding.mediaControlButton.setImageResource(R.drawable.ic_play_arrow)
                binding.waveformView.cancelAnimation()
                binding.waveformView.progress = 0f
                binding.speedTextView.isVisible = false
            }

            binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onPlayPauseClicked(message) }
            binding.speedTextView.setOnClickListener { if (!isSelectionMode) listener.onSpeedClicked(message) }
            handleSelection(itemView, message, this)
            // handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class IncomingFileViewHolder(val binding: ItemChatMessageFileIncomingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            val fileMessage = message.fileMessage!!
            binding.senderNameTextView.text = message.senderName
            binding.fileNameTextView.text = fileMessage.fileName
            binding.fileSizeTextView.text = android.text.format.Formatter.formatShortFileSize(itemView.context, fileMessage.fileSize)
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            binding.mediaProgressBar.isVisible = false
            binding.mediaControlButton.isVisible = true

            when (fileMessage.status) {
                TrackStatus.DOWNLOADING -> {
                    binding.mediaProgressBar.progress = fileMessage.progress
                    binding.mediaProgressBar.isVisible = true
                    binding.mediaControlButton.setImageResource(R.drawable.ic_cancel)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onCancelClicked(message) }
                }
                TrackStatus.READY -> {
                    binding.mediaControlButton.setImageResource(R.drawable.ic_file_open)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onFileClicked(message) }
                }
                else -> {
                    binding.mediaControlButton.setImageResource(R.drawable.ic_download)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onDownloadClicked(message) }
                }
            }
            handleSelection(itemView, message, this)
            handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class OutgoingFileViewHolder(val binding: ItemChatMessageFileOutgoingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            val fileMessage = message.fileMessage!!
            binding.fileNameTextView.text = fileMessage.fileName
            binding.fileSizeTextView.text = android.text.format.Formatter.formatShortFileSize(itemView.context, fileMessage.fileSize)
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            binding.mediaProgressBar.isVisible = false
            binding.mediaControlButton.isVisible = true

            when (fileMessage.status) {
                TrackStatus.UPLOADING, TrackStatus.DOWNLOADING -> {
                    binding.mediaProgressBar.progress = fileMessage.progress
                    binding.mediaProgressBar.isVisible = true
                    binding.mediaControlButton.setImageResource(R.drawable.ic_cancel)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onCancelClicked(message) }
                }
                TrackStatus.READY -> {
                    binding.mediaControlButton.setImageResource(R.drawable.ic_file_open)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onFileClicked(message) }
                }
                TrackStatus.FAILED -> {
                    binding.mediaControlButton.setImageResource(R.drawable.ic_send_media)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onResendClicked(message) }
                }
                else -> {
                    binding.mediaControlButton.setImageResource(R.drawable.ic_file_open)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onFileClicked(message) }
                }
            }
            handleSelection(itemView, message, this)
            handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class IncomingImageViewHolder(val binding: ItemChatMessageImageIncomingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            val imageMessage = message.imageMessage!!
            binding.senderNameTextView.text = message.senderName
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            val isReadyLocally = imageMessage.localUri.isNotEmpty()
            val needsDownload = imageMessage.localUri.isEmpty() && imageMessage.imageUrl.isNotEmpty()

            val imageUriToLoad = if (isReadyLocally) imageMessage.localUri else imageMessage.imageUrl

            if (imageUriToLoad.isNotEmpty()) {
                Glide.with(itemView.context).load(imageUriToLoad).into(binding.messageImageView)
            }

            when (imageMessage.status) {
                TrackStatus.DOWNLOADING -> {
                    binding.blurOverlay.isVisible = true
                    binding.downloadContainer.isVisible = true
                    binding.mediaProgressBar.isVisible = true
                    binding.mediaControlButton.isVisible = true
                    binding.mediaControlButton.setImageResource(R.drawable.ic_cancel)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onCancelClicked(message) }
                    binding.mediaProgressBar.progress = imageMessage.progress
                }
                TrackStatus.IDLE -> {
                    if (needsDownload) {
                        binding.blurOverlay.isVisible = true
                        binding.downloadContainer.isVisible = true
                        binding.mediaProgressBar.isVisible = false
                        binding.mediaControlButton.isVisible = true
                        binding.mediaControlButton.setImageResource(R.drawable.ic_download)
                        binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onDownloadClicked(message) }
                    } else {
                        binding.blurOverlay.isVisible = false
                        binding.downloadContainer.isVisible = false
                    }
                }
                TrackStatus.READY -> {
                    binding.blurOverlay.isVisible = false
                    binding.downloadContainer.isVisible = false
                    binding.messageImageView.setOnClickListener { if (!isSelectionMode) listener.onImageClicked(message) }
                }
                else -> {
                    binding.blurOverlay.isVisible = false
                    binding.downloadContainer.isVisible = false
                }
            }
            handleSelection(itemView, message, this)
            handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class OutgoingImageViewHolder(val binding: ItemChatMessageImageOutgoingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            val imageMessage = message.imageMessage!!
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            val isUploading = imageMessage.status == TrackStatus.UPLOADING
            binding.mediaProgressBar.isVisible = isUploading
            if (isUploading) {
                binding.mediaProgressBar.progress = imageMessage.progress
            }

            val imageUri = if (imageMessage.localUri.isNotEmpty()) imageMessage.localUri else imageMessage.imageUrl
            Glide.with(itemView.context).load(imageUri).into(binding.messageImageView)

            if (imageMessage.status == TrackStatus.READY) {
                binding.messageImageView.setOnClickListener { if (!isSelectionMode) listener.onImageClicked(message) }
            } else {
                binding.messageImageView.setOnClickListener(null)
            }
            handleSelection(itemView, message, this)
            handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class IncomingVideoViewHolder(val binding: ItemChatMessageVideoIncomingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            val videoMessage = message.videoMessage!!
            binding.senderNameTextView.text = message.senderName
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            Glide.with(itemView.context).load(videoMessage.thumbnailUrl).into(binding.thumbnailImageView)

            when (videoMessage.status) {
                TrackStatus.DOWNLOADING -> {
                    binding.downloadContainer.isVisible = true
                    binding.mediaProgressBar.isVisible = true
                    binding.mediaControlButton.isVisible = true
                    binding.mediaControlButton.setImageResource(R.drawable.ic_cancel)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onCancelClicked(message) }
                    binding.mediaProgressBar.progress = videoMessage.progress
                    binding.playIcon.isVisible = false
                }
                TrackStatus.IDLE -> {
                    binding.downloadContainer.isVisible = true
                    binding.mediaProgressBar.isVisible = false
                    binding.mediaControlButton.isVisible = true
                    binding.mediaControlButton.setImageResource(R.drawable.ic_download)
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onDownloadClicked(message) }
                    binding.playIcon.isVisible = false
                }
                TrackStatus.READY -> {
                    binding.downloadContainer.isVisible = false
                    binding.playIcon.isVisible = true
                    binding.thumbnailImageView.setOnClickListener { if (!isSelectionMode) listener.onVideoClicked(message) }
                    binding.playIcon.setOnClickListener { if (!isSelectionMode) listener.onVideoClicked(message) }
                }
                else -> {
                    binding.downloadContainer.isVisible = false
                    binding.playIcon.isVisible = false
                }
            }
            handleSelection(itemView, message, this)
            handleReactions(message, binding.reactionsLayout)
        }
    }

    inner class OutgoingVideoViewHolder(val binding: ItemChatMessageVideoOutgoingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleReply(message, binding.replyLayout, binding.replySenderNameTextView, binding.replyMessagePreviewTextView)
            val videoMessage = message.videoMessage!!
            binding.timestampTextView.text = formatTimestamp(message.timestamp)
            val thumbnailUri = if (videoMessage.localUri.isNotEmpty()) videoMessage.localUri else videoMessage.thumbnailUrl
            Glide.with(itemView.context).load(thumbnailUri).into(binding.thumbnailImageView)

            when (videoMessage.status) {
                TrackStatus.UPLOADING -> {
                    binding.uploadContainer.isVisible = true
                    binding.mediaProgressBar.progress = videoMessage.progress
                    binding.mediaControlButton.setOnClickListener { if (!isSelectionMode) listener.onCancelClicked(message) }
                    binding.playIcon.isVisible = false
                }
                TrackStatus.READY -> {
                    binding.uploadContainer.isVisible = false
                    binding.playIcon.isVisible = true
                    binding.thumbnailImageView.setOnClickListener { if (!isSelectionMode) listener.onVideoClicked(message) }
                    binding.playIcon.setOnClickListener { if (!isSelectionMode) listener.onVideoClicked(message) }
                }
                else -> {
                    binding.uploadContainer.isVisible = false
                    binding.playIcon.isVisible = false
                }
            }
            handleSelection(itemView, message, this)
            handleReactions(message, binding.reactionsLayout)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val isOutgoing = message.senderId == currentUserId
        return when (MessageType.valueOf(message.type)) {
            MessageType.TEXT -> if (isOutgoing) VIEW_TYPE_TEXT_OUTGOING else VIEW_TYPE_TEXT_INCOMING
            MessageType.MUSIC -> if (isOutgoing) VIEW_TYPE_MUSIC_OUTGOING else VIEW_TYPE_MUSIC_INCOMING
            MessageType.VOICE -> if (isOutgoing) VIEW_TYPE_VOICE_OUTGOING else VIEW_TYPE_VOICE_INCOMING
            MessageType.FILE -> if (isOutgoing) VIEW_TYPE_FILE_OUTGOING else VIEW_TYPE_FILE_INCOMING
            MessageType.IMAGE -> if (isOutgoing) VIEW_TYPE_IMAGE_OUTGOING else VIEW_TYPE_IMAGE_INCOMING
            MessageType.VIDEO -> if (isOutgoing) VIEW_TYPE_VIDEO_OUTGOING else VIEW_TYPE_VIDEO_INCOMING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TEXT_OUTGOING -> OutgoingTextViewHolder(ItemChatMessageOutgoingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_TEXT_INCOMING -> IncomingTextViewHolder(ItemChatMessageIncomingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_MUSIC_OUTGOING -> OutgoingMusicViewHolder(ItemChatMessageMusicOutgoingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_MUSIC_INCOMING -> IncomingMusicViewHolder(ItemChatMessageMusicIncomingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_VOICE_OUTGOING -> OutgoingVoiceViewHolder(ItemChatMessageVoiceOutgoingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_VOICE_INCOMING -> IncomingVoiceViewHolder(ItemChatMessageVoiceIncomingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_FILE_OUTGOING -> OutgoingFileViewHolder(ItemChatMessageFileOutgoingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_FILE_INCOMING -> IncomingFileViewHolder(ItemChatMessageFileIncomingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_IMAGE_OUTGOING -> OutgoingImageViewHolder(ItemChatMessageImageOutgoingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_IMAGE_INCOMING -> IncomingImageViewHolder(ItemChatMessageImageIncomingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_VIDEO_OUTGOING -> OutgoingVideoViewHolder(ItemChatMessageVideoOutgoingBinding.inflate(inflater, parent, false))
            VIEW_TYPE_VIDEO_INCOMING -> IncomingVideoViewHolder(ItemChatMessageVideoIncomingBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads[0] == "PROGRESS_UPDATE") {
            val message = messages[position]
            when (holder) {
                is OutgoingMusicViewHolder -> holder.binding.mediaProgressBar.progress = message.musicTrack?.progress ?: 0
                is IncomingMusicViewHolder -> holder.binding.mediaProgressBar.progress = message.musicTrack?.progress ?: 0
                is OutgoingFileViewHolder -> holder.binding.mediaProgressBar.progress = message.fileMessage?.progress ?: 0
                is IncomingFileViewHolder -> holder.binding.mediaProgressBar.progress = message.fileMessage?.progress ?: 0
                is OutgoingImageViewHolder -> holder.binding.mediaProgressBar.progress = message.imageMessage?.progress ?: 0
                is IncomingImageViewHolder -> (holder as IncomingImageViewHolder).binding.mediaProgressBar.progress = message.imageMessage?.progress ?: 0
                is OutgoingVideoViewHolder -> (holder as OutgoingVideoViewHolder).binding.mediaProgressBar.progress = message.videoMessage?.progress ?: 0
                is IncomingVideoViewHolder -> (holder as IncomingVideoViewHolder).binding.mediaProgressBar.progress = message.videoMessage?.progress ?: 0
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is OutgoingTextViewHolder -> holder.bind(message)
            is IncomingTextViewHolder -> holder.bind(message)
            is OutgoingMusicViewHolder -> holder.bind(message)
            is IncomingMusicViewHolder -> holder.bind(message)
            is OutgoingVoiceViewHolder -> holder.bind(message)
            is IncomingVoiceViewHolder -> holder.bind(message)
            is OutgoingFileViewHolder -> holder.bind(message)
            is IncomingFileViewHolder -> holder.bind(message)
            is OutgoingImageViewHolder -> holder.bind(message)
            is IncomingImageViewHolder -> holder.bind(message)
            is OutgoingVideoViewHolder -> holder.bind(message)
            is IncomingVideoViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDuration(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
