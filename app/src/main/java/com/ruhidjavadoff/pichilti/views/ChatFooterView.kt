package com.ruhidjavadoff.pichilti.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.ruhidjavadoff.pichilti.databinding.ChatFooterBinding

class ChatFooterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ChatFooterBinding
    private var onSendClickListener: ((String) -> Unit)? = null

    init {
        binding = ChatFooterBinding.inflate(LayoutInflater.from(context), this, true)
        binding.actionButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                onSendClickListener?.invoke(messageText)
                binding.messageEditText.text.clear()
            }
        }
    }

    fun setOnSendClickListener(listener: (String) -> Unit) {
        this.onSendClickListener = listener
    }
}