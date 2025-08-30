package com.ruhidjavadoff.pichilti

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MediaRepository(private val context: Context) {

    suspend fun getAllMedia(): List<MediaItem> {
        return withContext(Dispatchers.IO) {
            // DÜZƏLİŞ: Sıralama üçün müvəqqəti siyahı yaradırıq (Tarix, Media Obyekti)
            val mediaListWithDate = mutableListOf<Pair<Long, MediaItem>>()

            // Şəkilləri tapmaq üçün sorğu
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
            )
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            context.contentResolver.query(
                imageCollection,
                imageProjection,
                null,
                null,
                null // Sıralamanı sonda özümüz edəcəyik
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val date = cursor.getLong(dateColumn)
                    val contentUri = Uri.withAppendedPath(imageCollection, id.toString())
                    // Tarixi də obyektlə birlikdə siyahıya əlavə edirik
                    mediaListWithDate.add(Pair(date, MediaItem(uri = contentUri, type = MediaType.IMAGE)))
                }
            }

            // Videoları tapmaq üçün sorğu
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DURATION
            )
            val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            context.contentResolver.query(
                videoCollection,
                videoProjection,
                null,
                null,
                null // Sıralamanı sonda özümüz edəcəyik
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val date = cursor.getLong(dateColumn)
                    val durationMillis = cursor.getLong(durationColumn)
                    val contentUri = Uri.withAppendedPath(videoCollection, id.toString())
                    // Tarixi də obyektlə birlikdə siyahıya əlavə edirik
                    mediaListWithDate.add(
                        Pair(date, MediaItem(
                            uri = contentUri,
                            type = MediaType.VIDEO,
                            duration = formatDuration(durationMillis)
                        ))
                    )
                }
            }

            // DÜZƏLİŞ: Birləşmiş siyahını tarixinə görə sıralayırıq
            return@withContext mediaListWithDate
                .sortedByDescending { it.first } // .first cütün ilk elementidir (yəni tarix)
                .map { it.second } // Yalnız ikinci elementi (MediaItem) götürürük
        }
    }

    private fun formatDuration(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
