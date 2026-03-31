package com.closetai.app.data.settings

import android.content.Context
import com.closetai.app.data.model.BackendUserProfile
import com.google.gson.Gson

object UserProfileCacheStore {
    private const val PREFS_NAME = "closetai_profile_cache"
    private const val KEY_BACKEND_PROFILE = "backend_profile_json"
    private val gson = Gson()

    fun getBackendProfile(context: Context): BackendUserProfile? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_BACKEND_PROFILE, null) ?: return null
        return try {
            gson.fromJson(raw, BackendUserProfile::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun setBackendProfile(context: Context, profile: BackendUserProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BACKEND_PROFILE, gson.toJson(profile)).apply()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_BACKEND_PROFILE).apply()
    }
}

