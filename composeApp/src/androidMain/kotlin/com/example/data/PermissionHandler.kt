package com.example.data

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object PermissionHandler {
    private var launcher: ActivityResultLauncher<Array<String>>? = null
    private var activity: ComponentActivity? = null
    private var pendingCallback: ((Boolean) -> Unit)? = null

    fun register(activity: ComponentActivity) {
        this.activity = activity
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val granted = results.values.all { it }
            pendingCallback?.invoke(granted)
            pendingCallback = null
        }
    }

    fun request(
        permissions: Array<String>,
        onResult: (Boolean) -> Unit
    ) {
        val act = activity ?: return
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(act, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            onResult(true)
            return
        }
        pendingCallback = onResult
        launcher?.launch(permissions)
    }
}
