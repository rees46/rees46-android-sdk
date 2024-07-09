package com.personalizatio.api.responses.product

import com.google.gson.annotations.SerializedName

data class ImageUrlResized(
    @SerializedName("120")
    val x120: String,
    @SerializedName("140")
    val x140: String,
    @SerializedName("160")
    val x160: String,
    @SerializedName("180")
    val x180: String,
    @SerializedName("200")
    val x200: String,
    @SerializedName("220")
    val x220: String,
    @SerializedName("310")
    val x310: String,
    @SerializedName("520")
    val x520: String
)
