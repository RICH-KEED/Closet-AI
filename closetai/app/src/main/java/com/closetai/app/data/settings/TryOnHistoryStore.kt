package com.closetai.app.data.settings

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class TryOnHistoryItem(
    val createdAt: Long,
    val title: String,
    val outputPath: String
)

object TryOnHistoryStore {
    private const val PREFS_NAME = "closetai_tryon_history"
    private const val KEY_ITEMS = "items"
    private val gson = Gson()

    fun get(context: Context): List<TryOnHistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val t = object : TypeToken<List<TryOnHistoryItem>>() {}.type
            gson.fromJson<List<TryOnHistoryItem>>(raw, t) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, item: TryOnHistoryItem) {
        val current = get(context).toMutableList()
        current.add(0, item)
        // keep last 30 for demo stability
        val trimmed = current.take(30)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ITEMS, gson.toJson(trimmed)).apply()
    }
}

