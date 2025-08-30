package com.ruhidjavadoff.pichilti.services

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ruhidjavadoff.pichilti.Message
import com.ruhidjavadoff.pichilti.MessageType

class MediaPlayerService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    enum class PlayerState {
        IDLE, PREPARING, PLAYING, PAUSED
    }

    private var mediaPlayer: MediaPlayer? = null
    private val binder = LocalBinder()

    private val _playerState = MutableLiveData<PlayerState>(PlayerState.IDLE)
    val playerState: LiveData<PlayerState> = _playerState

    // DƏYİŞİKLİK: Artıq MusicTrack yox, bütün Message obyektini izləyirik
    private val _currentlyPlayingMessage = MutableLiveData<Message?>()
    val currentlyPlayingMessage: LiveData<Message?> = _currentlyPlayingMessage

    private val _playbackPosition = MutableLiveData<Int>()
    val playbackPosition: LiveData<Int> = _playbackPosition

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var progressRunnable: Runnable

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener(this@MediaPlayerService)
            setOnErrorListener(this@MediaPlayerService)
            setOnCompletionListener {
                _playerState.postValue(PlayerState.IDLE)
                _currentlyPlayingMessage.postValue(null)
                if (this@MediaPlayerService::progressRunnable.isInitialized) {
                    handler.removeCallbacks(progressRunnable)
                }
            }
        }
    }

    // DƏYİŞİKLİK: Funksiya artıq Message obyekti qəbul edir
    fun playOrPause(message: Message) {
        val isCurrentlyPlaying = _currentlyPlayingMessage.value?.messageId == message.messageId && _playerState.value == PlayerState.PLAYING
        val isCurrentlyPaused = _currentlyPlayingMessage.value?.messageId == message.messageId && _playerState.value == PlayerState.PAUSED

        if (isCurrentlyPlaying) {
            pause()
        } else if (isCurrentlyPaused) {
            resume()
        } else {
            play(message)
        }
    }

    private fun play(message: Message) {
        try {
            val dataSourceUri: Uri? = when (MessageType.valueOf(message.type)) {
                MessageType.MUSIC -> {
                    val track = message.musicTrack
                    if (track?.localUri?.isNotEmpty() == true) Uri.parse(track.localUri)
                    else if (track?.trackUrl?.isNotEmpty() == true) Uri.parse(track.trackUrl)
                    else null
                }
                MessageType.VOICE -> {
                    val voice = message.voiceMessage
                    if (voice?.localUri?.isNotEmpty() == true) Uri.parse(voice.localUri)
                    else if (voice?.voiceUrl?.isNotEmpty() == true) Uri.parse(voice.voiceUrl)
                    else null
                }
                else -> null
            }

            if (dataSourceUri == null) {
                _playerState.postValue(PlayerState.IDLE)
                Toast.makeText(this, "Fayl mənbəyi tapılmadı", Toast.LENGTH_SHORT).show()
                return
            }

            _currentlyPlayingMessage.postValue(message)
            _playerState.postValue(PlayerState.PREPARING)
            mediaPlayer?.apply {
                reset()
                setDataSource(this@MediaPlayerService, dataSourceUri)
                prepareAsync()
            }
        } catch (e: Exception) {
            _playerState.postValue(PlayerState.IDLE)
            Toast.makeText(this, "Fayl oxunarkən xəta baş verdi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pause() {
        mediaPlayer?.pause()
        _playerState.postValue(PlayerState.PAUSED)
        if (this::progressRunnable.isInitialized) {
            handler.removeCallbacks(progressRunnable)
        }
    }

    private fun resume() {
        mediaPlayer?.start()
        _playerState.postValue(PlayerState.PLAYING)
        startUpdatingProgress()
    }

    // YENİ: Səsin sürətini dəyişmək üçün funksiya
    fun setPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    val params = it.playbackParams ?: PlaybackParams()
                    params.speed = speed
                    it.playbackParams = params
                }
            }
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp?.start()
        _playerState.postValue(PlayerState.PLAYING)
        startUpdatingProgress()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        _playerState.postValue(PlayerState.IDLE)
        _currentlyPlayingMessage.postValue(null)
        Toast.makeText(this, "Fayl oxunarkən xəta baş verdi", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun startUpdatingProgress() {
        progressRunnable = Runnable {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    _playbackPosition.postValue(it.currentPosition)
                    handler.postDelayed(progressRunnable, 1000)
                }
            }
        }
        handler.post(progressRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        if (this::progressRunnable.isInitialized) {
            handler.removeCallbacks(progressRunnable)
        }
    }
}
