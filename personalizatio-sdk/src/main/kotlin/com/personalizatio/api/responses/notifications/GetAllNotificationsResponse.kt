package com.personalizatio.api.responses.notifications


import com.google.gson.annotations.SerializedName

data class GetAllNotificationsResponse(
    @SerializedName("payload")
    val payload: Payload,
    @SerializedName("status")
    val status: String
)
