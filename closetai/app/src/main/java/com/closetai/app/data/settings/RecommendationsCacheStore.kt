package com.closetai.app.data.settings

import android.content.Context
import com.closetai.app.data.model.ProductRecommendation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest

object RecommendationsCacheStore {
    private const val PREFS_NAME = "closetai_recommendations_cache"
    private val gson = Gson()

    private fun cacheKey(userUid: String, contextParams: Map<String, String>?): String {
        val stableContext = contextParams
            ?.toSortedMap()
            ?.entries
            ?.joinToString(separator = "&") { (k, v) -> "${k.trim()}=${v.trim()}" }
            .orEmpty()
        val digest = MessageDigest.getInstance("MD5")
            .digest(stableContext.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "rec_${userUid}_$digest"
    }

    fun get(context: Context, userUid: String, contextParams: Map<String, String>?): List<ProductRecommendation> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(cacheKey(userUid, contextParams), null) ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<ProductRecommendation>>() {}.type
            gson.fromJson<List<ProductRecommendation>>(raw, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun set(
        context: Context,
        userUid: String,
        contextParams: Map<String, String>?,
        items: List<ProductRecommendation>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(cacheKey(userUid, contextParams), gson.toJson(items))
            .apply()
    }
}

