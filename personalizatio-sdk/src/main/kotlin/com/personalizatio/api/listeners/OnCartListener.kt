package com.personalizatio.api.listeners

import com.personalizatio.entities.products.cart.CartEntity

interface OnCartListener {

    fun onGetCart(cartEntity: CartEntity) {}
}
