package com.dramix.app.data.source.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class LicensePreferences(private val context: Context) {

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

    fun saveLicense(
        isVip: Boolean,
        token: String?,
        expiresAt: Long?,
        licenseKey: String?,
        planName: String?
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_VIP, isVip)
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRES_AT, expiresAt ?: -1L)
            .putString(KEY_LICENSE_KEY, licenseKey)
            .putString(KEY_PLAN_NAME, planName)
            .apply()
    }

    fun isVipActive(): Boolean {
        val isVip = prefs.getBoolean(KEY_IS_VIP, false)
        if (!isVip) return false

        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, -1L)
        // If expiresAt is -1 (lifetime) or in the future
        return expiresAt == -1L || expiresAt > System.currentTimeMillis()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getLicenseKey(): String? = prefs.getString(KEY_LICENSE_KEY, null)

    fun getExpiresAt(): Long? {
        val exp = prefs.getLong(KEY_EXPIRES_AT, -1L)
        return if (exp == -1L) null else exp
    }

    fun getPlanName(): String? = prefs.getString(KEY_PLAN_NAME, null)

    fun clearLicense() {
        prefs.edit().clear().apply()
    }

    val vipStatusFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_IS_VIP || key == KEY_EXPIRES_AT) {
                trySend(isVipActive())
            }
        }
        trySend(isVipActive())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()

    companion object {
        private const val PREFS_NAME = "dramix_secure_license_prefs"
        private const val PREFS_FALLBACK_NAME = "dramix_license_prefs_fallback"

        private const val KEY_IS_VIP = "key_is_vip"
        private const val KEY_TOKEN = "key_token"
        private const val KEY_EXPIRES_AT = "key_expires_at"
        private const val KEY_LICENSE_KEY = "key_license_key"
        private const val KEY_PLAN_NAME = "key_plan_name"
    }
}
