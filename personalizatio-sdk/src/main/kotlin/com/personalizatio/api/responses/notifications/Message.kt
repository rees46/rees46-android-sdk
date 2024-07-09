package com.personalizatio.api.responses.notifications


import com.google.gson.annotations.SerializedName

data class Message(
    @SerializedName("body")
    val body: String?,
    @SerializedName("campaign_id")
    val campaignId: Int,
    @SerializedName("channel")
    val channel: String,
    @SerializedName("code")
    val code: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("icon")
    val icon: String?,
    @SerializedName("picture")
    val picture: String?,
    @SerializedName("sent_at")
    val sentAt: String?,
    @SerializedName("statistics")
    val statistics: Statistics,
    @SerializedName("subject")
    val subject: String?,
    @SerializedName("type")
    val type: String,
    @SerializedName("url")
    val url: String?
)
