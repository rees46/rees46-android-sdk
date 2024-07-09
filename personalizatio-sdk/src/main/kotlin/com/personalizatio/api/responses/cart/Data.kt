package com.personalizatio.api.responses.cart

import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("items")
    val items: List<Item>
)
