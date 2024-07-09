package com.personalizatio.api.responses.search

import com.google.gson.annotations.SerializedName

data class Suggest(
    @SerializedName("name")
    val name: String,
    @SerializedName("url")
    val url: String
)
