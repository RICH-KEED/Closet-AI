package com.closetai.app.data.model

import com.google.gson.annotations.SerializedName

data class WardrobeItem(
    val id: String = "",
    val userId: String = "",
    @SerializedName("image_url")
    val imageUrl: String = "",
    val category: String = "",
    val color: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
