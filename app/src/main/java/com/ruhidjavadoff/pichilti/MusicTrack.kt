package com.ruhidjavadoff.pichilti

import android.os.Parcelable
import com.google.firebase.database.Exclude
import kotlinx.parcelize.Parcelize

// DİQQƏT: Təkrarlanan "enum class TrackStatus" buradan silindi.
// O, artıq Message.kt faylından istifadə ediləcək.

@Parcelize
data class MusicTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val duration: Long = 0L,
    var imageUrl: String = "",
    var trackUrl: String = "", // Bu, Firebase Storage-dakı son ünvan olacaq

    // Bu sahələr Firebase-ə yazılmamalıdır
    @get:Exclude @set:Exclude var localUri: String = "",
    @get:Exclude @set:Exclude var status: TrackStatus = TrackStatus.IDLE,
    @get:Exclude @set:Exclude var progress: Int = 0
) : Parcelable
