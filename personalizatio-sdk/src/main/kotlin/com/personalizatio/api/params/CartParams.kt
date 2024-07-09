package com.personalizatio.api.params

import com.personalizatio.AbstractParams
import com.personalizatio.features.cart.ClearCartParameter

class CartParams : AbstractParams<CartParams>() {

    internal fun put(parameter: ClearCartParameter, value: String): CartParams {
        return put(parameter, value)
    }
}
