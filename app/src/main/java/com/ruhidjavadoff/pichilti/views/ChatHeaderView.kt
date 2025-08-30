package com.ruhidjavadoff.pichilti.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.appbar.MaterialToolbar
import com.ruhidjavadoff.pichilti.R
import com.ruhidjavadoff.pichilti.databinding.ChatHeaderContentBinding

class ChatHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialToolbar(context, attrs, defStyleAttr) {

    private val binding: ChatHeaderContentBinding

    init {
        // Geri düyməsini əlavə edirik
        setNavigationIcon(R.drawable.ic_arrow_back)
        // Başlığın içindəki məzmunu (yeni yaratdığımız faylı) yükləyirik
        binding = ChatHeaderContentBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setRoomInfo(name: String, participantInfo: String) {
        binding.toolbarRoomName.text = name
        binding.toolbarParticipantCount.text = participantInfo
    }

    fun setOnMenuClickListener(listener: OnClickListener) {
        binding.menuButton.setOnClickListener(listener)
    }
}