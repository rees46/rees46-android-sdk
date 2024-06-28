package com.personalizatio.entities.products.cart


import com.google.gson.annotations.SerializedName

data class CartEntity(
    @SerializedName("data")
    val data: Data,
    @SerializedName("status")
    val status: String
)
