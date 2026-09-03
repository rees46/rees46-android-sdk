package com.personalization.api.models.purchase

/**
 * Wire (JSON) keys for `push` purchase tracking. Public API uses camelCase; these are snake_case only here.
 */
internal object PurchaseTrackingWireKeys {
    const val EVENT = "event"
    const val PURCHASE_EVENT_VALUE = "purchase"
    const val ITEMS = "items"
    const val ID = "id"
    const val AMOUNT = "amount"
    const val PRICE = "price"
    const val QUANTITY = "quantity"
    const val LINE_ID = "line_id"
    const val FASHION_SIZE = "fashion_size"
    const val ORDER_ID = "order_id"
    const val ORDER_PRICE = "order_price"
    const val DELIVERY_TYPE = "delivery_type"
    const val DELIVERY_ADDRESS = "delivery_address"
    const val PAYMENT_TYPE = "payment_type"
    const val TAX_FREE = "tax_free"
    const val GIFT_PACKAGE = "gift_package"
    const val PROMOCODE = "promocode"
    const val ORDER_CASH = "order_cash"
    const val ORDER_BONUSES = "order_bonuses"
    const val ORDER_DELIVERY = "order_delivery"
    const val ORDER_DISCOUNT = "order_discount"
    const val CHANNEL = "channel"
    const val CUSTOM = "custom"
    const val RECOMMENDED_SOURCE = "recommended_source"
}
