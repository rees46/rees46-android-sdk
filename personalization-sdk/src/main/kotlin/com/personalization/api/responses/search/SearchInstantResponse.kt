package com.personalization.api.responses.search

import com.google.gson.annotations.SerializedName
import com.personalization.api.responses.product.Product

data class SearchInstantResponse(
    @SerializedName("book_author")
    val bookAuthor: List<Any>,
    val categories: List<Category>,
    val clarification: Boolean,
    val collections: List<Any>,
    val html: String,
    val products: List<Product>,
    val locations: List<Location> = emptyList(),
    @SerializedName("products_total")
    val productsTotal: Int,
    val queries: List<Any>,
    @SerializedName("requests_count")
    val requestsCount: Int,
    @SerializedName("search_query")
    val searchQuery: String,
    @SerializedName("search_query_original")
    val searchQueryOriginal: String = ""
)
