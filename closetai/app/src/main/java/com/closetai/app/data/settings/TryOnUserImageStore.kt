package com.closetai.app.data.settings

import android.content.Context

object TryOnUserImageStore {
    private const val PREFS_NAME = "closetai_tryon"
    private const val KEY_USER_IMAGE_PATH = "user_image_path"

    fun getUserImagePath(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_IMAGE_PATH, null)
    }

    fun setUserImagePath(context: Context, path: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_IMAGE_PATH, path).apply()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_USER_IMAGE_PATH).apply()
    }
}

