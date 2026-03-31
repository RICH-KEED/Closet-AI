package com.closetai.app.data.settings

import android.content.Context
import com.closetai.app.data.model.ProductRecommendation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SavedItemsStore {
    private const val PREFS_NAME = "closetai_saved_items"
    private val gson = Gson()

    private fun keyForUser(userUid: String): String = "saved_items_$userUid"

    fun getSavedItems(context: Context, userUid: String): List<ProductRecommendation> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(keyForUser(userUid), null) ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<ProductRecommendation>>() {}.type
            gson.fromJson<List<ProductRecommendation>>(raw, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun upsertItem(context: Context, userUid: String, item: ProductRecommendation) {
        val current = getSavedItems(context, userUid).toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            current[idx] = item
        } else {
            current.add(0, item)
        }
        persist(context, userUid, current)
    }

    fun removeItem(context: Context, userUid: String, productId: String) {
        val current = getSavedItems(context, userUid).filterNot { it.id == productId }
        persist(context, userUid, current)
    }

    private fun persist(context: Context, userUid: String, items: List<ProductRecommendation>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(keyForUser(userUid), gson.toJson(items)).apply()
    }
}

