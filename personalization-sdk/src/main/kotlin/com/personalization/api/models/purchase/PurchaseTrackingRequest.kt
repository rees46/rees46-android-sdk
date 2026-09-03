package com.personalization.api.models.purchase

import com.personalization.Params
import org.json.JSONObject

/**
 * Strict purchase tracking request for `push` (event = `purchase`).
 *
 * Required: [orderId], [orderPrice], [items].
 * All other fields are optional — omit them from the constructor when unused (no need to pass `null`).
 */
data class PurchaseTrackingRequest(
    val orderId: String,
    val orderPrice: Double,
    val items: List<PurchaseItemRequest>,
    val deliveryType: String? = null,
    val deliveryAddress: String? = null,
    val paymentType: String? = null,
    val isTaxFree: Boolean = false,
    val promocode: String? = null,
    val orderCash: Double? = null,
    val orderBonuses: Double? = null,
    val orderDelivery: Double? = null,
    val orderDiscount: Double? = null,
    val channel: String? = null,
    val custom: Map<String, Any?>? = null,
    val recommendedBy: Params.RecommendedBy? = null,
    val recommendedSource: JSONObject? = null,
    val stream: String? = null,
    val segment: String? = null,
    val isGiftPackage: Boolean = false,
)
