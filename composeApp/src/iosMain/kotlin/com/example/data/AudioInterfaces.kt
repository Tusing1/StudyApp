package com.example.data

actual class AudioPlayer {
    actual val isPlaying: Boolean = false
    actual val currentPositionMs: Long = 0L
    actual val durationMs: Long = 0L
    
    actual fun play(url: String) {}
    actual fun pause() {}
    actual fun seekTo(positionMs: Long) {}
    actual fun setPlaybackSpeed(speed: Float) {}
    actual fun setVolume(volume: Float) {}
    actual fun release() {}
}

actual class AudioRecorder {
    actual val isRecording: Boolean = false
    actual val isPaused: Boolean = false
    actual val maxAmplitude: Float = 0f
    actual val durationSeconds: Int = 0

    actual fun startRecording(filePath: String) {}
    actual fun pauseRecording() {}
    actual fun resumeRecording() {}
    actual fun stopRecording() {}
    actual fun release() {}
}

actual fun getTempDirectoryPath(): String {
    return ""
}

actual fun checkAndRequestRecordPermission(onGranted: () -> Unit) {
    onGranted()
}

actual fun isPermissionGranted(permissionType: String): Boolean {
    return true
}

actual fun requestPermission(permissionType: String, onResult: (Boolean) -> Unit) {
    onResult(true)
}

actual fun readFileBytes(filePath: String): ByteArray {
    return ByteArray(0)
}

actual fun deleteFile(filePath: String): Boolean {
    return false
}

