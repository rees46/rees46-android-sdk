package com.personalizatio.entities.products.productInfo


import com.google.gson.annotations.SerializedName

data class ImageDownloadData(
    @SerializedName("could_be_widgetable")
    val couldBeWidgetable: Boolean,
    @SerializedName("image")
    val image: String,
    @SerializedName("image_changed")
    val imageChanged: Boolean,
    @SerializedName("name")
    val name: String,
    @SerializedName("persisted")
    val persisted: Boolean,
    @SerializedName("price")
    val price: String,
    @SerializedName("time")
    val time: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("widgetable")
    val widgetable: Boolean
)