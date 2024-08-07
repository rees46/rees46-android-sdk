package com.personalization.api.responses.recommendation

import com.google.gson.annotations.SerializedName
import com.personalization.api.responses.product.Product

data class GetExtendedRecommendationResponse(
    @SerializedName("html")
    val html: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("recommends")
    val products: List<Product>,
    @SerializedName("title")
    val title: String
)
