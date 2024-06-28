package com.personalizatio.entities.products.cart


import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("uniqid")
    val uniqid: String
)
