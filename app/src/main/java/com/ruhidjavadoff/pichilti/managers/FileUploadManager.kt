package com.ruhidjavadoff.pichilti.managers

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.ktx.storage
import com.ruhidjavadoff.pichilti.Message
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

object FileUploadManager {

    private val storage = Firebase.storage
    private val activeUploads = mutableMapOf<String, StorageTask<*>>()
    private val activeDownloads = mutableMapOf<String, FileDownloadTask>()


    interface UploadCallback {
        fun onProgress(progress: Int)
        fun onSuccess(downloadUrl: Uri)
        fun onFailure(exception: Exception)
    }

    interface VideoUploadCallback {
        fun onProgress(progress: Int)
        fun onSuccess(videoUrl: Uri, thumbnailUrl: Uri)
        fun onFailure(exception: Exception)
    }


    interface DownloadCallback {
        fun onProgress(progress: Int)
        fun onSuccess(localFileUri: Uri)
        fun onFailure(exception: Exception)
    }

    suspend fun uploadVideoFile(context: Context, message: Message, fileUri: Uri, callback: VideoUploadCallback) {
        val videoFileName = "${fileUri.lastPathSegment}_${System.currentTimeMillis()}"
        val videoStorageRef = storage.reference.child("videos/$videoFileName")
        val thumbnailStorageRef = storage.reference.child("video_thumbnails/thumb_$videoFileName.jpg")

        try {
            val thumbnailBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(fileUri, Size(360, 360), null)
            } else {
                // DÜZƏLİŞ: Köhnə versiyalar üçün fayl yolundan istifadə edirik
                @Suppress("DEPRECATION")
                val filePath = getPathFromUri(context, fileUri)
                if (filePath != null) {
                    ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND)
                } else {
                    null
                }
            }

            val baos = ByteArrayOutputStream()
            thumbnailBitmap?.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val thumbnailData = baos.toByteArray()

            val thumbnailUrl = thumbnailStorageRef.putBytes(thumbnailData).await().storage.downloadUrl.await()

            val uploadTask = videoStorageRef.putFile(fileUri)
            activeUploads[message.messageId] = uploadTask

            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                callback.onProgress(progress)
            }.addOnSuccessListener {
                videoStorageRef.downloadUrl.addOnSuccessListener { videoUrl ->
                    activeUploads.remove(message.messageId)
                    callback.onSuccess(videoUrl, thumbnailUrl)
                }
            }.addOnFailureListener { exception ->
                activeUploads.remove(message.messageId)
                callback.onFailure(exception)
            }

        } catch (e: Exception) {
            callback.onFailure(e)
        }
    }

    // YENİ KÖMƏKÇİ FUNKSİYA
    private fun getPathFromUri(context: Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    path = it.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            // Xəta baş verərsə, heç nə etmirik
        }
        return path
    }


    fun uploadImageFile(message: Message, fileUri: Uri, callback: UploadCallback) {
        val storageRef = storage.reference.child("images/${fileUri.lastPathSegment}_${System.currentTimeMillis()}")
        val uploadTask = storageRef.putFile(fileUri)
        activeUploads[message.messageId] = uploadTask

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            callback.onProgress(progress)
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                activeUploads.remove(message.messageId)
                callback.onSuccess(downloadUri)
            }
        }.addOnFailureListener { exception ->
            activeUploads.remove(message.messageId)
            callback.onFailure(exception)
        }
    }

    fun uploadMusicFile(message: Message, fileUri: Uri, callback: UploadCallback) {
        val storageRef = storage.reference.child("music/${fileUri.lastPathSegment}_${System.currentTimeMillis()}")
        val uploadTask = storageRef.putFile(fileUri)
        activeUploads[message.messageId] = uploadTask

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            callback.onProgress(progress)
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                activeUploads.remove(message.messageId)
                callback.onSuccess(downloadUri)
            }
        }.addOnFailureListener { exception ->
            activeUploads.remove(message.messageId)
            callback.onFailure(exception)
        }
    }

    fun uploadVoiceFile(message: Message, fileUri: Uri, callback: UploadCallback) {
        val storageRef = storage.reference.child("voice_messages/${fileUri.lastPathSegment}_${System.currentTimeMillis()}")
        val uploadTask = storageRef.putFile(fileUri)
        activeUploads[message.messageId] = uploadTask

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                activeUploads.remove(message.messageId)
                callback.onSuccess(downloadUri)
            }
        }.addOnFailureListener { exception ->
            activeUploads.remove(message.messageId)
            callback.onFailure(exception)
        }
    }

    fun uploadGeneralFile(message: Message, fileUri: Uri, callback: UploadCallback) {
        val fileName = message.fileMessage?.fileName ?: "file_${System.currentTimeMillis()}"
        val storageRef = storage.reference.child("files/$fileName")

        val uploadTask = storageRef.putFile(fileUri)
        activeUploads[message.messageId] = uploadTask

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            callback.onProgress(progress)
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                activeUploads.remove(message.messageId)
                callback.onSuccess(downloadUri)
            }
        }.addOnFailureListener { exception ->
            activeUploads.remove(message.messageId)
            callback.onFailure(exception)
        }
    }

    fun downloadVideoFile(context: Context, message: Message, callback: DownloadCallback) {
        val videoMessage = message.videoMessage ?: return
        if (videoMessage.videoUrl.isEmpty()) {
            callback.onFailure(Exception("Video URL is empty"))
            return
        }

        val storageRef = storage.getReferenceFromUrl(videoMessage.videoUrl)
        val localFile = File(context.filesDir, "${message.messageId}.mp4")

        val downloadTask = storageRef.getFile(localFile)
        activeDownloads[message.messageId] = downloadTask

        downloadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            callback.onProgress(progress)
        }.addOnSuccessListener {
            activeDownloads.remove(message.messageId)
            callback.onSuccess(Uri.fromFile(localFile))
        }.addOnFailureListener { exception ->
            activeDownloads.remove(message.messageId)
            if (localFile.exists()) {
                localFile.delete()
            }
            callback.onFailure(exception)
        }
    }

    fun downloadImageFile(context: Context, message: Message, callback: DownloadCallback) {
        val imageMessage = message.imageMessage ?: return
        if (imageMessage.imageUrl.isEmpty()) {
            callback.onFailure(Exception("Image URL is empty"))
            return
        }

        val storageRef = storage.getReferenceFromUrl(imageMessage.imageUrl)
        val localFile = File(context.filesDir, "${message.messageId}.jpg")

        val downloadTask = storageRef.getFile(localFile)
        activeDownloads[message.messageId] = downloadTask

        downloadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            callback.onProgress(progress)
        }.addOnSuccessListener {
            activeDownloads.remove(message.messageId)
            callback.onSuccess(Uri.fromFile(localFile))
        }.addOnFailureListener { exception ->
            activeDownloads.remove(message.messageId)
            if (localFile.exists()) {
                localFile.delete()
            }
            callback.onFailure(exception)
        }
    }

    fun downloadMusicFile(context: Context, message: Message, callback: DownloadCallback) {
        val track = message.musicTrack ?: return
        if (track.trackUrl.isEmpty()) {
            callback.onFailure(Exception("Track URL is empty"))
            return
        }

        val storageRef = storage.getReferenceFromUrl(track.trackUrl)
        val localFile = File(context.filesDir, "${message.messageId}.mp3")

        val downloadTask = storageRef.getFile(localFile)
        activeDownloads[message.messageId] = downloadTask

        downloadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            callback.onProgress(progress)
        }.addOnSuccessListener {
            activeDownloads.remove(message.messageId)
            callback.onSuccess(Uri.fromFile(localFile))
        }.addOnFailureListener { exception ->
            activeDownloads.remove(message.messageId)
            if (localFile.exists()) {
                localFile.delete()
            }
            callback.onFailure(exception)
        }
    }

    fun downloadGeneralFile(context: Context, message: Message, callback: DownloadCallback) {
        val fileMessage = message.fileMessage ?: return
        if (fileMessage.fileUrl.isEmpty()) {
            callback.onFailure(Exception("File URL is empty"))
            return
        }

        val storageRef = storage.getReferenceFromUrl(fileMessage.fileUrl)
        val localFile = File(context.filesDir, fileMessage.fileName)

        val downloadTask = storageRef.getFile(localFile)
        activeDownloads[message.messageId] = downloadTask

        downloadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            callback.onProgress(progress)
        }.addOnSuccessListener {
            activeDownloads.remove(message.messageId)
            callback.onSuccess(Uri.fromFile(localFile))
        }.addOnFailureListener { exception ->
            activeDownloads.remove(message.messageId)
            if (localFile.exists()) {
                localFile.delete()
            }
            callback.onFailure(exception)
        }
    }

    fun cancelUpload(messageId: String) {
        activeUploads[messageId]?.cancel()
        activeUploads.remove(messageId)
    }

    fun cancelDownload(messageId: String) {
        activeDownloads[messageId]?.cancel()
        activeDownloads.remove(messageId)
    }
}
