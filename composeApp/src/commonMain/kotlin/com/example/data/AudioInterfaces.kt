package com.example.data

expect class AudioPlayer() {
    val isPlaying: Boolean
    val currentPositionMs: Long
    val durationMs: Long
    
    fun play(url: String)
    fun pause()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun release()
}

expect class AudioRecorder() {
    val isRecording: Boolean
    val isPaused: Boolean
    val maxAmplitude: Float // 0f to 1f
    val durationSeconds: Int

    fun startRecording(filePath: String)
    fun pauseRecording()
    fun resumeRecording()
    fun stopRecording()
    fun release()
}

expect fun getTempDirectoryPath(): String

expect fun checkAndRequestRecordPermission(onGranted: () -> Unit)

expect fun isPermissionGranted(permissionType: String): Boolean
expect fun requestPermission(permissionType: String, onResult: (Boolean) -> Unit)

expect fun readFileBytes(filePath: String): ByteArray

expect fun deleteFile(filePath: String): Boolean

