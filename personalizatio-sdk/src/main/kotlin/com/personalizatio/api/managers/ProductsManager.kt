package com.personalizatio.api.managers

import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.listeners.OnProductsListener

interface ProductsManager {

    /**
     * Request a product info
     *
     * @param productId Product ID
     * @param listener Callback
     */
    fun getProductInfo(productId: String, listener: OnProductsListener)

    /**
     * Request a product info
     *
     * @param productId Product ID
     * @param listener Callback
     */
    fun getProductInfo(productId: String, listener: OnApiCallbackListener)
}
