package com.personalizatio.api.responses.notifications


import com.google.gson.annotations.SerializedName

data class Payload(
    @SerializedName("messages")
    val messages: List<Message>
)
