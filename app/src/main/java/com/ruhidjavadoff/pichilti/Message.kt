package com.ruhidjavadoff.pichilti

import android.os.Parcelable
import com.google.firebase.database.Exclude
import kotlinx.parcelize.Parcelize

enum class TrackStatus {
    IDLE,
    UPLOADING,
    DOWNLOADING,
    READY,
    FAILED
}

enum class MessageType {
    TEXT,
    MUSIC,
    VOICE,
    FILE,
    IMAGE,
    VIDEO
}

@Parcelize
data class ReplyInfo(
    val repliedToMessageId: String = "",
    val repliedToSenderName: String = "",
    val repliedToMessagePreview: String = ""
) : Parcelable

@Parcelize
data class Message(
    val messageId: String = "",
    val text: String? = null,
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = 0L,
    val type: String = MessageType.TEXT.name,
    var musicTrack: MusicTrack? = null,
    var voiceMessage: VoiceMessage? = null,
    var fileMessage: FileMessage? = null,
    var imageMessage: ImageMessage? = null,
    var videoMessage: VideoMessage? = null,
    var replyInfo: ReplyInfo? = null,
    // YENİ SAHƏ: Reaksiyaları saxlamaq üçün (Key: UserID, Value: Emoji)
    val reactions: Map<String, String> = emptyMap()
) : Parcelable

@Parcelize
data class VoiceMessage(
    var voiceUrl: String = "",
    var duration: Long = 0L,
    @get:Exclude @set:Exclude var localUri: String = ""
) : Parcelable

@Parcelize
data class FileMessage(
    var fileUrl: String = "",
    var fileName: String = "",
    var fileSize: Long = 0L,
    @get:Exclude @set:Exclude var localUri: String = "",
    @get:Exclude @set:Exclude var status: TrackStatus = TrackStatus.IDLE,
    @get:Exclude @set:Exclude var progress: Int = 0
) : Parcelable

@Parcelize
data class ImageMessage(
    var imageUrl: String = "",
    @get:Exclude @set:Exclude var localUri: String = "",
    @get:Exclude @set:Exclude var status: TrackStatus = TrackStatus.IDLE,
    @get:Exclude @set:Exclude var progress: Int = 0
) : Parcelable

@Parcelize
data class VideoMessage(
    var videoUrl: String = "",
    var thumbnailUrl: String = "",
    @get:Exclude @set:Exclude var localUri: String = "",
    @get:Exclude @set:Exclude var status: TrackStatus = TrackStatus.IDLE,
    @get:Exclude @set:Exclude var progress: Int = 0
) : Parcelable
