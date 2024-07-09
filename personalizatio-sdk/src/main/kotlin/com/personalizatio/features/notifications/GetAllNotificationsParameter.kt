package com.personalizatio.features.notifications

import com.personalizatio.AbstractParams

enum class GetAllNotificationsParameter(override var value: String) : AbstractParams.ParamInterface {
    EMAIL("email"),
    PHONE("phone"),
    EXTERNAL_ID("external_id"),
    LOYALTY_ID("loyalty_id"),
    DATE_FROM("date_from"),
    TYPE("type"),
    CHANNEL("channel"),
    PAGE("page"),
    LIMIT("limit")
}