package com.personalizatio.api.responses.categories.categories

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("children")
    val childrenCategories: List<Category>,
    @SerializedName("external_id")
    val externalId: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("parent_external_id")
    val parentExternalId: Any,
    @SerializedName("parent_id")
    val parentId: Any,
    @SerializedName("url")
    val url: String
)
