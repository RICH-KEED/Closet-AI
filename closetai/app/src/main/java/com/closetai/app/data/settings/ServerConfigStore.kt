package com.closetai.app.data.settings

import android.content.Context

object ServerConfigStore {
    private const val PREFS_NAME = "closetai_prefs"
    private const val KEY_BASE_URL = "base_url"

    fun getBaseUrl(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, null)
    }

    fun setBaseUrl(context: Context, baseUrl: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, baseUrl).apply()
    }
}

