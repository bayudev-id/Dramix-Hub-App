package com.dramix.app.core.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.util.UUID

class DeviceIdentifier(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(PREFS_FALLBACK_NAME, Context.MODE_PRIVATE)
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        val cached = prefs.getString(KEY_DEVICE_ID, null)
        if (!cached.isNullOrBlank()) {
            return cached
        }

        val rawId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            null
        }

        val input = if (!rawId.isNullOrBlank() && rawId != "9774d56d682e549c") {
            rawId
        } else {
            UUID.randomUUID().toString()
        }

        val hashed = sha256("DRAMIX_DEVICE_$input")
        prefs.edit().putString(KEY_DEVICE_ID, hashed).apply()
        return hashed
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "dramix_secure_device_prefs"
        private const val PREFS_FALLBACK_NAME = "dramix_device_prefs_fallback"
        private const val KEY_DEVICE_ID = "cached_hardware_fingerprint"
    }
}
