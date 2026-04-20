package com.closetai.app.data.model

import com.google.gson.annotations.SerializedName

data class WardrobeUploadResponse(
    @SerializedName("status") val status: String,
    @SerializedName("image_url") val imageUrl: String
)

