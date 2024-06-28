package com.personalizatio.api.listeners

import com.personalizatio.entities.products.productInfo.ProductInfoEntity

interface OnProductsListener {

    fun onGetProductInfo(productInfoEntity: ProductInfoEntity) {}
}
