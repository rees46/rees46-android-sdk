package com.personalizatio.api.responses.notifications


import com.google.gson.annotations.SerializedName

data class Statistics(
    @SerializedName("clicked")
    val clicked: Boolean,
    @SerializedName("complained")
    val complained: Boolean,
    @SerializedName("hard_bounced")
    val hardBounced: Boolean,
    @SerializedName("opened")
    val opened: Boolean,
    @SerializedName("purchased")
    val purchased: Boolean,
    @SerializedName("soft_bounced")
    val softBounced: Boolean,
    @SerializedName("unsubscribed")
    val unsubscribed: Boolean
)
