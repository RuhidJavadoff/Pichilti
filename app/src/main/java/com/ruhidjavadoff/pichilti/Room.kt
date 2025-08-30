package com.ruhidjavadoff.pichilti

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Room(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val animationAsset: String = "",
    val participantCount: Int = 0, // Bu, ümumi üzv sayı olaraq qala bilər
    // YENİ SAHƏLƏR
    val fakeParticipantBase: Int = 0, // 5-15 arası təsadüfi əsas rəqəm
    val fakeParticipantTimestamp: Long = 0L // Bu rəqəmin nə vaxt yaradıldığını saxlayır
) : Parcelable