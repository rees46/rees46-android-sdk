package com.personalizatio.api.responses.shared

import com.google.gson.annotations.SerializedName

data class PriceRange(
    @SerializedName("max")
    val max: Double,
    @SerializedName("min")
    val min: Double
)
