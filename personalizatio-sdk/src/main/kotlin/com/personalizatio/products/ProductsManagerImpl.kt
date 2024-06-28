package com.personalizatio.products

import com.google.gson.Gson
import com.personalizatio.Params
import com.personalizatio.SDK
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.listeners.OnProductsListener
import com.personalizatio.api.managers.ProductsManager
import com.personalizatio.entities.products.productInfo.ProductInfoEntity
import org.json.JSONObject

internal class ProductsManagerImpl(val sdk: SDK) : ProductsManager {

    override fun getProductInfo(productId: String, listener: OnProductsListener) {
        getProductInfo(productId, object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    val productInfoEntity = Gson().fromJson(it.toString(), ProductInfoEntity::class.java)
                    listener.onGetProductInfo(productInfoEntity)
                }
            }
        })
    }

    override fun getProductInfo(productId: String, listener: OnApiCallbackListener) {
        val params = Params().put(Params.Parameter.ITEM_ID, productId)
        sdk.getAsync(GET_PRODUCT_INFO_REQUEST, params.build(), listener)
    }

    companion object {
        const val GET_PRODUCT_INFO_REQUEST = "products/get/"
    }
}
