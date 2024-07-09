package com.personalizatio.api.responses.cart

import com.google.gson.annotations.SerializedName

data class GetCartResponse(
    @SerializedName("data")
    val data: Data,
    @SerializedName("status")
    val status: String
)
