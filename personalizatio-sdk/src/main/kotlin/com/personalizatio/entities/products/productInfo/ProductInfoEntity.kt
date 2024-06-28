package com.personalizatio.entities.products.productInfo


import com.google.gson.annotations.SerializedName

data class ProductInfoEntity(
    @SerializedName("brand")
    val brand: String,
    @SerializedName("brand_downcase")
    val brandDowncase: String,
    @SerializedName("categories")
    val categories: List<Category>,
    @SerializedName("category_ids")
    val categoryIds: List<String>,
    @SerializedName("child_age_max")
    val childAgeMax: Int,
    @SerializedName("child_age_min")
    val childAgeMin: Int,
    @SerializedName("child_gender")
    val childGender: String,
    @SerializedName("child_type")
    val childType: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("discount")
    val discount: Boolean,
    @SerializedName("ignored")
    val ignored: Boolean,
    @SerializedName("image_download_data")
    val imageDownloadData: ImageDownloadData,
    @SerializedName("image_download_start_at")
    val imageDownloadStartAt: String,
    @SerializedName("is_available")
    val isAvailable: Boolean,
    @SerializedName("is_child")
    val isChild: Boolean,
    @SerializedName("locations")
    val locations: Locations,
    @SerializedName("name")
    val name: String,
    @SerializedName("picture")
    val picture: String,
    @SerializedName("price")
    val price: String,
    @SerializedName("sales_rate")
    val salesRate: Int,
    @SerializedName("uniqid")
    val uniqid: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("widgetable")
    val widgetable: Boolean
)