package com.personalizatio.api.responses.cart

import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("uniqid")
    val uniqid: String
)
