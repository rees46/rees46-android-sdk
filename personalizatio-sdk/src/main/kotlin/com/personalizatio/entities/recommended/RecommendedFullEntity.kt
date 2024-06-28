package com.personalizatio.entities.recommended

import com.google.gson.annotations.SerializedName

data class RecommendedFullEntity(
    @SerializedName("html")
    val html: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("recommends")
    val recommends: List<Recommend>,
    @SerializedName("title")
    val title: String
)
