package com.closetai.app.data.settings

import android.content.Context

object TryOnUserImageStore {
    private const val PREFS_NAME = "closetai_tryon"
    private const val KEY_USER_IMAGE_URI = "user_image_uri"

    fun getUserImageUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_IMAGE_URI, null)
    }

    fun setUserImageUri(context: Context, uri: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_IMAGE_URI, uri).apply()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_USER_IMAGE_URI).apply()
    }
}

