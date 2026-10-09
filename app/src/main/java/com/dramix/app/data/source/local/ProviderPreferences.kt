package com.dramix.app.data.source.local

import android.content.Context
import android.content.SharedPreferences
import com.dramix.app.domain.model.ProviderModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class UserProviderConfig(
    val id: String,
    val isEnabled: Boolean = true
)

data class ProviderConfigItem(
    val provider: ProviderModel,
    val isEnabled: Boolean
)

class ProviderPreferences(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _configsFlow = MutableStateFlow<List<UserProviderConfig>>(emptyList())
    val configsFlow: Flow<List<UserProviderConfig>> = _configsFlow.asStateFlow()

    init {
        _configsFlow.value = loadConfigsFromPrefs()
    }

    private fun loadConfigsFromPrefs(): List<UserProviderConfig> {
        val rawJson = prefs.getString(KEY_CONFIGS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(rawJson)
            val list = mutableListOf<UserProviderConfig>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val enabled = obj.optBoolean("enabled", true)
                if (id.isNotBlank()) {
                    list.add(UserProviderConfig(id = id, isEnabled = enabled))
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getSavedConfigs(): List<UserProviderConfig> = loadConfigsFromPrefs()

    fun saveConfigs(configs: List<UserProviderConfig>) {
        val jsonArray = JSONArray()
        for (cfg in configs) {
            val obj = JSONObject().apply {
                put("id", cfg.id)
                put("enabled", cfg.isEnabled)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_CONFIGS, jsonArray.toString()).apply()
        _configsFlow.value = configs
    }

    fun resetToDefault() {
        prefs.edit().remove(KEY_CONFIGS).apply()
        _configsFlow.value = emptyList()
    }

    /**
     * Applies user-defined order and on/off filter to a list of raw providers.
     * New providers not yet in user configs are appended at the end and enabled by default.
     * If user disabled all providers, falls back to rawProviders so the feed is not empty.
     */
    fun applyToProviders(rawProviders: List<ProviderModel>): List<ProviderModel> {
        val saved = loadConfigsFromPrefs()
        if (saved.isEmpty()) return rawProviders

        val providerMap = rawProviders.associateBy { it.id }
        val result = mutableListOf<ProviderModel>()
        val seenIds = mutableSetOf<String>()

        for (cfg in saved) {
            val prov = providerMap[cfg.id]
            if (prov != null) {
                seenIds.add(prov.id)
                if (cfg.isEnabled) {
                    result.add(prov)
                }
            }
        }

        // Add any remaining raw providers not yet stored in user preferences
        for (prov in rawProviders) {
            if (prov.id !in seenIds) {
                result.add(prov)
            }
        }

        return if (result.isEmpty()) rawProviders else result
    }

    /**
     * Prepares the full list of providers with their ordering and enabled states for the UI customizer.
     */
    fun getMergedConfigItems(rawProviders: List<ProviderModel>): List<ProviderConfigItem> {
        val saved = loadConfigsFromPrefs()
        if (saved.isEmpty()) {
            return rawProviders.map { ProviderConfigItem(provider = it, isEnabled = true) }
        }

        val providerMap = rawProviders.associateBy { it.id }
        val result = mutableListOf<ProviderConfigItem>()
        val seenIds = mutableSetOf<String>()

        for (cfg in saved) {
            val prov = providerMap[cfg.id]
            if (prov != null) {
                seenIds.add(prov.id)
                result.add(ProviderConfigItem(provider = prov, isEnabled = cfg.isEnabled))
            }
        }

        for (prov in rawProviders) {
            if (prov.id !in seenIds) {
                result.add(ProviderConfigItem(provider = prov, isEnabled = true))
            }
        }

        return result
    }

    fun saveLastSelection(
        contentType: String?,
        providerId: String?,
        categoryId: String?
    ) {
        prefs.edit().apply {
            putString(KEY_LAST_CONTENT_TYPE, contentType)
            putString(KEY_LAST_PROVIDER_ID, providerId)
            putString(KEY_LAST_CATEGORY_ID, categoryId)
            apply()
        }
    }

    fun getLastContentType(): String? = prefs.getString(KEY_LAST_CONTENT_TYPE, null)

    fun getLastProviderId(): String? = prefs.getString(KEY_LAST_PROVIDER_ID, null)

    fun getLastCategoryId(): String? = prefs.getString(KEY_LAST_CATEGORY_ID, null)

    companion object {
        private const val PREFS_NAME = "dramix_provider_prefs"
        private const val KEY_CONFIGS = "user_provider_configs"
        private const val KEY_LAST_CONTENT_TYPE = "last_selected_content_type"
        private const val KEY_LAST_PROVIDER_ID = "last_selected_provider_id"
        private const val KEY_LAST_CATEGORY_ID = "last_selected_category_id"
    }
}
