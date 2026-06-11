package com.example.data

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

actual class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false

    actual val isPlaying: Boolean
        get() = try { mediaPlayer?.isPlaying == true } catch (e: Exception) { false }

    actual val currentPositionMs: Long
        get() = try { 
            if (isPrepared) mediaPlayer?.currentPosition?.toLong() ?: 0L else 0L 
        } catch (e: Exception) { 0L }

    actual val durationMs: Long
        get() = try { 
            if (isPrepared) mediaPlayer?.duration?.toLong() ?: 0L else 0L 
        } catch (e: Exception) { 0L }

    actual fun play(url: String) {
        try {
            release()
            isPrepared = false
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener {
                    isPrepared = true
                    start()
                }
                setOnCompletionListener {
                    // completed
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun pause() {
        try {
            if (isPlaying) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun seekTo(positionMs: Long) {
        try {
            if (isPrepared) {
                mediaPlayer?.seekTo(positionMs.toInt())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun setPlaybackSpeed(speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    val params = player.playbackParams.setSpeed(speed)
                    player.playbackParams = params
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun setVolume(volume: Float) {
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun release() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            isPrepared = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

actual class AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var _isRecording = false
    private var _isPaused = false
    private var startTimeMs: Long = 0L
    private var pausedTimeMs: Long = 0L
    private var accumulatedDurationMs: Long = 0L

    actual val isRecording: Boolean
        get() = _isRecording

    actual val isPaused: Boolean
        get() = _isPaused

    actual val maxAmplitude: Float
        get() = try {
            val maxAmp = mediaRecorder?.maxAmplitude ?: 0
            (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
        } catch (e: Exception) {
            0f
        }

    actual val durationSeconds: Int
        get() {
            if (!_isRecording) return 0
            val elapsed = if (_isPaused) {
                accumulatedDurationMs
            } else {
                accumulatedDurationMs + (System.currentTimeMillis() - startTimeMs)
            }
            return (elapsed / 1000).toInt()
        }

    actual fun startRecording(filePath: String) {
        try {
            release()
            
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(DatabaseProvider.getContext())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(filePath)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                prepare()
                start()
            }
            
            _isRecording = true
            _isPaused = false
            startTimeMs = System.currentTimeMillis()
            accumulatedDurationMs = 0L
        } catch (e: Exception) {
            e.printStackTrace()
            _isRecording = false
        }
    }

    actual fun pauseRecording() {
        try {
            if (_isRecording && !_isPaused) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.pause()
                    _isPaused = true
                    pausedTimeMs = System.currentTimeMillis()
                    accumulatedDurationMs += (pausedTimeMs - startTimeMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun resumeRecording() {
        try {
            if (_isRecording && _isPaused) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.resume()
                    _isPaused = false
                    startTimeMs = System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun stopRecording() {
        try {
            if (_isRecording) {
                mediaRecorder?.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            release()
        }
    }

    actual fun release() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            _isRecording = false
            _isPaused = false
            accumulatedDurationMs = 0L
        }
    }
}

actual fun getTempDirectoryPath(): String {
    return DatabaseProvider.getContext().cacheDir.absolutePath
}

actual fun checkAndRequestRecordPermission(onGranted: () -> Unit) {
    val context = DatabaseProvider.getContext()
    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    if (hasPermission) {
        onGranted()
    } else {
        PermissionHandler.request(
            arrayOf(android.Manifest.permission.RECORD_AUDIO)
        ) { granted -> if (granted) onGranted() }
    }
}

actual fun isPermissionGranted(permissionType: String): Boolean {
    val context = DatabaseProvider.getContext()
    return when (permissionType.lowercase()) {
        "microphone" -> {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        "camera" -> {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        "storage" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
        else -> false
    }
}

actual fun requestPermission(permissionType: String, onResult: (Boolean) -> Unit) {
    val permissions = when (permissionType.lowercase()) {
        "microphone" -> arrayOf(android.Manifest.permission.RECORD_AUDIO)
        "camera" -> arrayOf(android.Manifest.permission.CAMERA)
        "storage" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_AUDIO
                )
            } else {
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
        else -> return
    }
    PermissionHandler.request(permissions, onResult)
}

actual fun readFileBytes(filePath: String): ByteArray {
    return java.io.File(filePath).readBytes()
}

actual fun deleteFile(filePath: String): Boolean {
    return try {
        java.io.File(filePath).delete()
    } catch (e: Exception) {
        false
    }
}

