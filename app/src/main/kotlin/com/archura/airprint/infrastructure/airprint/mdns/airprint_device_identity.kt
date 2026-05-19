package com.archura.airprint.infrastructure.airprint.mdns

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirPrintDeviceIdentity @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val uuid: String = preferences.getString(KEY_UUID, null)
        ?: UUID.randomUUID().toString().also { generatedUuid ->
            preferences.edit().putString(KEY_UUID, generatedUuid).apply()
        }

    fun hostName(): String {
        return "archura-airprint-${uuid.take(UUID_PREFIX_LENGTH)}"
            .lowercase(Locale.US)
    }

    private companion object {
        const val KEY_UUID = "uuid"
        const val PREFS_NAME = "airprint_device_identity"
        const val UUID_PREFIX_LENGTH = 8
    }
}
