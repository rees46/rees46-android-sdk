package com.personalizatio.features.cart

import com.google.gson.Gson
import com.personalizatio.Params
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.responses.cart.GetCartResponse
import com.personalizatio.api.managers.CartManager
import com.personalizatio.api.managers.NetworkManager
import com.personalizatio.api.params.CartParams
import org.json.JSONObject

internal class CartManagerImpl(private val networkManager: NetworkManager) : CartManager {

    override fun getCart(
        onGetCart: (GetCartResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        getCart(object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    val getCartResponse = Gson().fromJson(it.toString(), GetCartResponse::class.java)
                    onGetCart(getCartResponse)
                }
            }

            override fun onError(code: Int, msg: String?) {
                onError(code, msg)
            }
        })
    }

    override fun getCart(
        listener: OnApiCallbackListener
    ) {
        networkManager.getAsync(GET_CART_REQUEST, Params().build(), listener)
    }

    override fun clearCart(
        email: String?,
        phone: String?,
        loyaltyId: String?,
        externalId: String?,
        telegramId: String?,
        listener: OnApiCallbackListener?
    ) {
        val params = CartParams()
        if (email != null) {
            params.put(ClearCartParameter.EMAIL, email)
        }
        if (phone != null) {
            params.put(ClearCartParameter.PHONE, phone)
        }
        if (externalId != null) {
            params.put(ClearCartParameter.EXTERNAL_ID, externalId)
        }
        if (loyaltyId != null) {
            params.put(ClearCartParameter.LOYALTY_ID, loyaltyId)
        }
        if (telegramId != null) {
            params.put(ClearCartParameter.TELEGRAM_ID, telegramId)
        }
        networkManager.postSecretAsync(CLEAR_CART_REQUEST, params.build(), listener)
    }

    companion object {
        const val GET_CART_REQUEST = "products/cart"
        const val CLEAR_CART_REQUEST = "products/cart/clear"
    }
}
