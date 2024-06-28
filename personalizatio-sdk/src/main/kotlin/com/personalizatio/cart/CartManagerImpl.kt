package com.personalizatio.cart

import com.google.gson.Gson
import com.personalizatio.Params
import com.personalizatio.SDK
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.listeners.OnCartListener
import com.personalizatio.api.managers.CartManager
import com.personalizatio.entities.products.cart.CartEntity
import org.json.JSONObject

internal class CartManagerImpl(val sdk: SDK) : CartManager {

    override fun getCart(listener: OnCartListener) {
        getCart(object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    val cartEntity = Gson().fromJson(it.toString(), CartEntity::class.java)
                    listener.onGetCart(cartEntity)
                }
            }
        })
    }

    override fun getCart(listener: OnApiCallbackListener) {
        sdk.getAsync(GET_CART_REQUEST, Params().build(), listener)
    }

    override fun addToCart(productId: String, quantity: Int) {
        val params = createProductParams(productId, quantity)
        addToCart(params)
    }

    override fun addToCart(products: Map<String, Int>) {
        val params = createProductsParams(products)
        addToCart(params)
    }

    override fun addToCart(params: Params) {
        sdk.track(Params.TrackEvent.CART, params)
    }

    override fun removeFromCart(productId: String, quantity: Int, listener: OnApiCallbackListener?) {
        val params = createProductParams(productId, quantity)
        removeFromCart(params, listener)
    }

    override fun removeFromCart(products: Map<String, Int>, listener: OnApiCallbackListener?) {
        val params = createProductsParams(products)
        removeFromCart(params, listener)
    }

    override fun removeFromCart(params: Params, listener: OnApiCallbackListener?) {
        sdk.track(Params.TrackEvent.REMOVE_FROM_CART, params, listener)
    }

    private fun createProductItem(productId: String, quantity: Int) : Params.Item {
        return Params.Item(productId).set(Params.Item.COLUMN.AMOUNT, quantity)
    }

    private fun createProductParams(productId: String, quantity: Int) : Params {
        return Params().put(createProductItem(productId, quantity))
    }

    private fun createProductsParams(products: Map<String, Int>) : Params {
        val params = Params()
        for (product in products) {
            params.put(createProductItem(product.key, product.value))
        }
        return params
    }

    companion object {
        const val GET_CART_REQUEST = "products/cart"
    }
}
