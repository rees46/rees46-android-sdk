package com.personalizatio.entities.recommended

import com.google.gson.annotations.SerializedName

data class Recommend(
    @SerializedName("brand")
    val brand: String,
    @SerializedName("categories")
    val categories: List<Category>,
    @SerializedName("category_ids")
    val categoryIds: List<String>,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("_id")
    val _id: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("image_url_handle")
    val imageUrlHandle: String,
    @SerializedName("image_url_resized")
    val imageUrlResized: ImageUrlResized,
    @SerializedName("name")
    val name: String,
    @SerializedName("picture")
    val picture: String,
    @SerializedName("price")
    val price: Int,
    @SerializedName("price_formatted")
    val priceFormatted: String,
    @SerializedName("price_full")
    val priceFull: Int,
    @SerializedName("price_full_formatted")
    val priceFullFormatted: String,
    @SerializedName("relative_sales_rate")
    val relativeSalesRate: Double,
    @SerializedName("sales_rate")
    val salesRate: Int,
    @SerializedName("url")
    val url: String,
    @SerializedName("url_handle")
    val urlHandle: String
)