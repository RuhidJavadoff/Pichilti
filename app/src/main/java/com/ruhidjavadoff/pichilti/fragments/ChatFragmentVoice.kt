package com.ruhidjavadoff.pichilti.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.ruhidjavadoff.pichilti.Message
import com.ruhidjavadoff.pichilti.MessageType
import com.ruhidjavadoff.pichilti.VoiceMessage
import com.ruhidjavadoff.pichilti.managers.FileUploadManager
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

internal fun ChatFragment.startRecording() {
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        return
    }

    isRecording = true
    voiceRecorder.startRecording()
    updateRecordingUI(true)
    startRecordingTimer()
}

internal fun ChatFragment.stopRecording(shouldSend: Boolean) {
    if (!isRecording) return

    isRecording = false
    val filePath = voiceRecorder.stopRecording()
    updateRecordingUI(false)
    stopRecordingTimer()

    if (shouldSend && filePath != null) {
        uploadVoiceFile(filePath)
    }
}

private fun ChatFragment.startRecordingTimer() {
    recordingStartTime = System.currentTimeMillis()
    recordingTimerRunnable = object : Runnable {
        override fun run() {
            val millis = System.currentTimeMillis() - recordingStartTime
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            binding.chatFooter.recordingTimeTextView.text = String.format("%d:%02d", minutes, seconds)
            recordingTimerRunnable?.let { recordingHandler.postDelayed(it, 1000) }
        }
    }
    recordingHandler.post(recordingTimerRunnable!!)
}

private fun ChatFragment.stopRecordingTimer() {
    recordingTimerRunnable?.let { recordingHandler.removeCallbacks(it) }
    binding.chatFooter.recordingTimeTextView.text = "0:00"
}

private fun ChatFragment.uploadVoiceFile(filePath: String) {
    val fileUri = File(filePath).toUri()
    val duration = getAudioDuration(filePath)

    val tempMessage = Message(
        messageId = UUID.randomUUID().toString(),
        senderId = currentUsername,
        senderName = currentUsername,
        timestamp = System.currentTimeMillis(),
        type = MessageType.VOICE.name,
        voiceMessage = VoiceMessage(
            duration = duration,
            localUri = fileUri.toString()
        )
    )

    messageList.add(tempMessage)
    chatAdapter?.notifyItemInserted(messageList.size - 1)
    binding.chatRecyclerView.scrollToPosition(messageList.size - 1)

    FileUploadManager.uploadVoiceFile(tempMessage, fileUri, object : FileUploadManager.UploadCallback {
        override fun onProgress(progress: Int) { }

        override fun onSuccess(downloadUrl: Uri) {
            activity?.runOnUiThread {
                val tempId = tempMessage.messageId
                val newMessageId = messagesRef.push().key!!

                val finalMessageForFirebase = tempMessage.copy(
                    messageId = newMessageId,
                    voiceMessage = tempMessage.voiceMessage?.copy(
                        voiceUrl = downloadUrl.toString(),
                        localUri = ""
                    )
                )

                messagesRef.child(newMessageId).setValue(finalMessageForFirebase)

                val index = messageList.indexOfFirst { it.messageId == tempId }
                if (index != -1) {
                    messageList[index] = finalMessageForFirebase.copy(
                        voiceMessage = finalMessageForFirebase.voiceMessage?.copy(
                            localUri = tempMessage.voiceMessage!!.localUri
                        )
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
                Toast.makeText(context, "Səsli mesaj göndərilə bilmədi", Toast.LENGTH_SHORT).show()
            }
        }
    })
}
