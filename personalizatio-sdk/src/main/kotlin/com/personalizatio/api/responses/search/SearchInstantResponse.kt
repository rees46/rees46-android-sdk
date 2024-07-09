package com.personalizatio.api.responses.search

import com.google.gson.annotations.SerializedName
import com.personalizatio.api.responses.product.Product

data class SearchInstantResponse(
    @SerializedName("book_author")
    val bookAuthor: List<Any>,
    @SerializedName("categories")
    val categories: List<Category>,
    @SerializedName("clarification")
    val clarification: Boolean,
    @SerializedName("collections")
    val collections: List<Any>,
    @SerializedName("html")
    val html: String,
    @SerializedName("products")
    val products: List<Product>,
    @SerializedName("products_total")
    val productsTotal: Int,
    @SerializedName("queries")
    val queries: List<Any>,
    @SerializedName("requests_count")
    val requestsCount: Int,
    @SerializedName("search_query")
    val searchQuery: String
)
