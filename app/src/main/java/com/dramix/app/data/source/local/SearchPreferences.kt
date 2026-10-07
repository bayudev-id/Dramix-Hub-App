package com.dramix.app.data.source.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchPreferences(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _queriesFlow = MutableStateFlow<List<String>>(emptyList())

    init {
        _queriesFlow.value = loadFromPrefs()
    }

    private fun loadFromPrefs(): List<String> {
        val raw = prefs.getString(KEY_QUERIES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(DELIMITER).filter { it.isNotBlank() }
    }

    fun getRecentQueries(): List<String> = loadFromPrefs()

    fun saveQuery(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return

        val current = loadFromPrefs().toMutableList()
        current.removeAll { it.equals(clean, ignoreCase = true) }
        current.add(0, clean)
        val trimmed = current.take(MAX_QUERIES)

        prefs.edit()
            .putString(KEY_QUERIES, trimmed.joinToString(DELIMITER))
            .apply()

        _queriesFlow.value = trimmed
    }

    fun removeQuery(query: String) {
        val current = loadFromPrefs().toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        prefs.edit()
            .putString(KEY_QUERIES, current.joinToString(DELIMITER))
            .apply()

        _queriesFlow.value = current
    }

    fun clearAll() {
        prefs.edit().remove(KEY_QUERIES).apply()
        _queriesFlow.value = emptyList()
    }

    fun observeRecentQueries(): Flow<List<String>> = _queriesFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "dramix_search_prefs"
        private const val KEY_QUERIES = "recent_search_queries"
        private const val DELIMITER = "\u001F" // Unit separator to avoid issues with punctuation
        private const val MAX_QUERIES = 10
    }
}
