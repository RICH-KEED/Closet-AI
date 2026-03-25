package com.closetai.app.data.model

import com.google.gson.annotations.SerializedName

data class ProductRecommendation(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("brand") val brand: String,
    @SerializedName("price") val price: Double,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("product_url") val productUrl: String,
    @SerializedName("platform") val platform: String,
    @SerializedName("match_score") val matchScore: Double,
    @SerializedName("match_reasons") val matchReasons: List<String> = emptyList()
)

data class RecommendationRequest(
    @SerializedName("user_profile") val userProfile: BackendUserProfile,
    @SerializedName("context") val context: Map<String, String>? = null
)

// We define a separate model for the backend request to match Python's UserProfile model exactly.
data class BackendUserProfile(
    @SerializedName("uid") val uid: String,
    @SerializedName("gender") val gender: String?,
    @SerializedName("bodyType") val bodyType: String?,
    @SerializedName("clothingSize") val clothingSize: String?,
    @SerializedName("budget") val budget: String?,
    @SerializedName("styles") val styles: List<String> = emptyList(),
    @SerializedName("favoriteColors") val favoriteColors: List<String> = emptyList(),
    @SerializedName("occasions") val occasions: List<String> = emptyList(),
    @SerializedName("wardrobe") val wardrobe: List<WardrobeItem> = emptyList()
)

fun User.toBackendUserProfile(wardrobeItems: List<WardrobeItem> = emptyList()): BackendUserProfile {
    return BackendUserProfile(
        uid = this.uid,
        gender = this.gender.takeIf { it.isNotEmpty() },
        bodyType = this.bodyType.takeIf { it.isNotEmpty() },
        clothingSize = this.clothingSize.takeIf { it.isNotEmpty() },
        budget = this.budget.takeIf { it.isNotEmpty() },
        styles = this.styles,
        favoriteColors = this.favoriteColors,
        occasions = this.occasions,
        wardrobe = wardrobeItems
    )
}
