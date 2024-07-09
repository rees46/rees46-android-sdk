package com.personalizatio.features.cart

import com.personalizatio.AbstractParams.ParamInterface

enum class ClearCartParameter(override var value: String) : ParamInterface {
    EMAIL("email"),
    PHONE("phone"),
    EXTERNAL_ID("external_id"),
    LOYALTY_ID("loyalty_id"),
    TELEGRAM_ID("telegram_id")
}