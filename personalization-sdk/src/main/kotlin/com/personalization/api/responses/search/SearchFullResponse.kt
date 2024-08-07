package com.personalization.api.responses.search

import com.google.gson.annotations.SerializedName
import com.personalization.api.responses.product.Product

data class SearchFullResponse(
    @SerializedName("brands")
    val brands: List<Brand>,
    @SerializedName("categories")
    val categories: List<Category>,
    @SerializedName("clarification")
    val clarification: Boolean,
    @SerializedName("collections")
    val collections: List<Any>,
    @SerializedName("html")
    val html: String,
    @SerializedName("price_median")
    val priceMedian: Double,
    @SerializedName("price_range")
    val priceRange: PriceRange,
    @SerializedName("price_ranges")
    val priceRanges: List<PriceRanges>,
    @SerializedName("products")
    val products: List<Product>,
    @SerializedName("products_total")
    val productsTotal: Int,
    @SerializedName("requests_count")
    val requestsCount: Int,
    @SerializedName("search_query")
    val searchQuery: String
)
