package com.archura.airprint.infrastructure.permission

import android.Manifest
import android.os.Build

object PermissionManager {
    fun requiredRuntimePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }
    }
}
