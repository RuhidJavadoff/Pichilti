package com.ruhidjavadoff.pichilti

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileModel(
    val uri: Uri,
    val name: String,
    val size: Long
) : Parcelable
