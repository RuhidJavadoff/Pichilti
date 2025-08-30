package com.ruhidjavadoff.pichilti.fragments

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.ruhidjavadoff.pichilti.FileMessage
import com.ruhidjavadoff.pichilti.Message
import com.ruhidjavadoff.pichilti.MessageType
import com.ruhidjavadoff.pichilti.TrackStatus
import com.ruhidjavadoff.pichilti.managers.FileUploadManager
import java.io.File
import java.util.*

// Bu fayl ChatFragment-in fayl əməliyyatları ilə bağlı məntiqini saxlayır.

/**
 * Musiqi faylını endirmək üçün prosesi başladır.
 */
internal fun ChatFragment.downloadMusic(message: Message) {
    val index = messageList.indexOfFirst { it.messageId == message.messageId }
    if (index == -1) return

    // Endirmə başladığını bildirmək üçün statusu dəyişirik
    messageList[index].musicTrack?.status = TrackStatus.DOWNLOADING
    chatAdapter?.notifyItemChanged(index)

    FileUploadManager.downloadMusicFile(requireContext(), message, object : FileUploadManager.DownloadCallback {
        override fun onProgress(progress: Int) {
            activity?.runOnUiThread {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].musicTrack?.progress = progress
                    chatAdapter?.notifyItemChanged(currentIndex, "PROGRESS_UPDATE")
                }
            }
        }

        override fun onSuccess(localFileUri: Uri) {
            activity?.runOnUiThread {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].musicTrack?.apply {
                        localUri = localFileUri.toString()
                        status = TrackStatus.READY
                    }
                    chatAdapter?.notifyItemChanged(currentIndex)
                }
            }
        }

        override fun onFailure(exception: Exception) {
            activity?.runOnUiThread {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].musicTrack?.status = TrackStatus.IDLE
                    chatAdapter?.notifyItemChanged(currentIndex)
                    Toast.makeText(context, "Endirmə uğursuz oldu.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    })
}

/**
 * Ümumi faylı endirmək üçün prosesi başladır.
 */
internal fun ChatFragment.downloadFile(message: Message) {
    val index = messageList.indexOfFirst { it.messageId == message.messageId }
    if (index == -1) return

    // Endirmə başladığını bildirmək üçün statusu dəyişirik
    messageList[index].fileMessage?.status = TrackStatus.DOWNLOADING
    chatAdapter?.notifyItemChanged(index)

    FileUploadManager.downloadGeneralFile(requireContext(), message, object : FileUploadManager.DownloadCallback {
        override fun onProgress(progress: Int) {
            activity?.runOnUiThread {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].fileMessage?.progress = progress
                    chatAdapter?.notifyItemChanged(currentIndex, "PROGRESS_UPDATE")
                }
            }
        }

        override fun onSuccess(localFileUri: Uri) {
            activity?.runOnUiThread {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].fileMessage?.apply {
                        localUri = localFileUri.toString()
                        status = TrackStatus.READY
                    }
                    chatAdapter?.notifyItemChanged(currentIndex)
                }
            }
        }

        override fun onFailure(exception: Exception) {
            activity?.runOnUiThread {
                val currentIndex = messageList.indexOfFirst { it.messageId == message.messageId }
                if (currentIndex != -1) {
                    messageList[currentIndex].fileMessage?.status = TrackStatus.IDLE
                    chatAdapter?.notifyItemChanged(currentIndex)
                    Toast.makeText(context, "Endirmə uğursuz oldu.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    })
}

/**
 * Endirilmiş faylı açmaq üçün sistem tətbiqlərini çağırır.
 */
internal fun ChatFragment.openFile(fileUri: Uri, fileName: String) {
    try {
        val file = File(fileUri.path!!)
        val contentUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val mimeType = requireContext().contentResolver.getType(contentUri)

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(openIntent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Bu faylı aça biləcək tətbiq tapılmadı.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Faylı açmaq mümkün olmadı.", Toast.LENGTH_LONG).show()
    }
}
