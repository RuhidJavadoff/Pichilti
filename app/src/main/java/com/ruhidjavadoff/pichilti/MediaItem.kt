package com.ruhidjavadoff.pichilti

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItem(
    val uri: Uri,
    val type: MediaType,
    val duration: String? = null // Yalnız videolar üçün
) : Parcelable

enum class MediaType {
    IMAGE,
    VIDEO
}
