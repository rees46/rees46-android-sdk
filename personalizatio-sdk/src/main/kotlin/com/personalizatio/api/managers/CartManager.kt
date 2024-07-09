package com.personalizatio.api.managers

import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.responses.cart.GetCartResponse

interface CartManager {

    /**
     * Request a cart
     *
     * @param onGetCart Callback for get cart
     * @param onError Callback for error
     */
    fun getCart(
        onGetCart: (GetCartResponse) -> Unit,
        onError: (Int, String?) -> Unit = { _: Int, _: String? -> }
    )

    /**
     * Request a cart
     *
     * @param listener Callback
     */
    fun getCart(listener: OnApiCallbackListener)

    /**
     * Request a clear cart.
     * At least of identifiers must present in request: email, phone, loyaltyId, externalId, telegramId. It's used to identify user.
     *
     * @param email Email, if available
     * @param phone Phone, if available
     * @param loyaltyId Loyalty ID, if available
     * @param externalId External ID, if available
     * @param telegramId Telegram ID, if available
     * @param listener Callback
     */
    fun clearCart(
        email: String? = null,
        phone: String? = null,
        loyaltyId: String? = null,
        externalId: String? = null,
        telegramId: String? = null,
        listener: OnApiCallbackListener? = null
    )
}
