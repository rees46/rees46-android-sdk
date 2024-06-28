package com.personalizatio.entities.products.productInfo


import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("branch")
    val branch: Int,
    @SerializedName("id")
    val id: String,
    @SerializedName("level")
    val level: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("name_with_parent")
    val nameWithParent: String,
    @SerializedName("parent_id")
    val parentId: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("url_handle")
    val urlHandle: String
)