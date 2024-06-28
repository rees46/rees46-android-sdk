package com.personalizatio.entities.recommended

import com.google.gson.annotations.SerializedName

data class RecommendedEntity(
    @SerializedName("html")
    val html: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("recommends")
    val productIds: List<Int>,
    @SerializedName("title")
    val title: String
)
