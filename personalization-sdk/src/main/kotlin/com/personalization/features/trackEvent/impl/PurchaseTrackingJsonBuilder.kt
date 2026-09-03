package com.personalization.features.trackEvent.impl

import com.personalization.Params
import com.personalization.api.models.purchase.PurchaseItemRequest
import com.personalization.api.models.purchase.PurchaseTrackingRequest
import com.personalization.api.models.purchase.PurchaseTrackingWireKeys
import com.personalization.sdk.data.models.params.UserBasicParams
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal object PurchaseTrackingJsonBuilder {

    const val CLIENT_VALIDATION_ERROR_CODE: Int = -2

    private val RESERVED_CUSTOM_KEYS: Set<String> = buildSet {
        addAll(TrackCustomEventPayloadHelper.RESERVED_CUSTOM_EVENT_KEYS)
        add(PurchaseTrackingWireKeys.EVENT)
        add(PurchaseTrackingWireKeys.ITEMS)
        add(PurchaseTrackingWireKeys.ORDER_ID)
        add(PurchaseTrackingWireKeys.ORDER_PRICE)
        add(PurchaseTrackingWireKeys.DELIVERY_TYPE)
        add(PurchaseTrackingWireKeys.DELIVERY_ADDRESS)
        add(PurchaseTrackingWireKeys.PAYMENT_TYPE)
        add(PurchaseTrackingWireKeys.TAX_FREE)
        add(PurchaseTrackingWireKeys.PROMOCODE)
        add(PurchaseTrackingWireKeys.ORDER_CASH)
        add(PurchaseTrackingWireKeys.ORDER_BONUSES)
        add(PurchaseTrackingWireKeys.ORDER_DELIVERY)
        add(PurchaseTrackingWireKeys.ORDER_DISCOUNT)
        add(PurchaseTrackingWireKeys.CHANNEL)
        add(PurchaseTrackingWireKeys.CUSTOM)
        add(PurchaseTrackingWireKeys.RECOMMENDED_SOURCE)
        add(Params.InternalParameter.RECOMMENDED_BY.value)
        add(Params.InternalParameter.RECOMMENDED_CODE.value)
    }

    fun buildOrError(request: PurchaseTrackingRequest): Result<JSONObject> {
        if (request.orderId.isBlank()) {
            return Result.failure(IllegalArgumentException("trackPurchase: orderId must be non-empty"))
        }
        if (request.items.isEmpty()) {
            return Result.failure(IllegalArgumentException("trackPurchase: items must not be empty"))
        }
        for (item in request.items) {
            if (item.id.isBlank()) {
                return Result.failure(IllegalArgumentException("trackPurchase: each item.id must be non-empty"))
            }
            if (item.amount <= 0) {
                return Result.failure(IllegalArgumentException("trackPurchase: each item.amount must be > 0"))
            }
            if (!item.price.isFinite()) {
                return Result.failure(IllegalArgumentException("trackPurchase: each item.price must be a finite number"))
            }
        }
        if (!request.orderPrice.isFinite()) {
            return Result.failure(IllegalArgumentException("trackPurchase: orderPrice must be a finite number"))
        }

        val effectiveCustom = TrackCustomEventPayloadHelper.effectiveCustomFields(request.custom)
        if (effectiveCustom.isNotEmpty()) {
            val collisions = effectiveCustom.keys.intersect(RESERVED_CUSTOM_KEYS)
            if (collisions.isNotEmpty()) {
                return Result.failure(
                    IllegalArgumentException(
                        "trackPurchase: custom contains reserved keys: ${collisions.toSortedSet().joinToString(", ")}"
                    )
                )
            }
        }

        return try {
            Result.success(buildJson(request, effectiveCustom))
        } catch (e: JSONException) {
            Result.failure(IllegalArgumentException("trackPurchase: failed to build JSON: ${e.message}", e))
        }
    }

    private fun buildJson(
        request: PurchaseTrackingRequest,
        effectiveCustom: Map<String, Any>,
    ): JSONObject {
        val root = JSONObject()
        root.put(PurchaseTrackingWireKeys.EVENT, PurchaseTrackingWireKeys.PURCHASE_EVENT_VALUE)
        root.put(PurchaseTrackingWireKeys.ORDER_ID, request.orderId)
        root.put(PurchaseTrackingWireKeys.ORDER_PRICE, request.orderPrice)

        val itemsArray = JSONArray()
        for (item in request.items) {
            itemsArray.put(itemToJson(item))
        }
        root.put(PurchaseTrackingWireKeys.ITEMS, itemsArray)

        request.deliveryType?.takeIf { it.isNotBlank() }?.let {
            root.put(PurchaseTrackingWireKeys.DELIVERY_TYPE, it)
        }
        request.deliveryAddress?.takeIf { it.isNotBlank() }?.let {
            root.put(PurchaseTrackingWireKeys.DELIVERY_ADDRESS, it)
        }
        request.paymentType?.takeIf { it.isNotBlank() }?.let {
            root.put(PurchaseTrackingWireKeys.PAYMENT_TYPE, it)
        }
        if (request.isTaxFree) {
            root.put(PurchaseTrackingWireKeys.TAX_FREE, true)
        }
        if (request.isGiftPackage) {
            root.put(PurchaseTrackingWireKeys.GIFT_PACKAGE, true)
        }
        request.promocode?.takeIf { it.isNotBlank() }?.let {
            root.put(PurchaseTrackingWireKeys.PROMOCODE, it)
        }
        request.orderCash?.let { root.put(PurchaseTrackingWireKeys.ORDER_CASH, it) }
        request.orderBonuses?.let { root.put(PurchaseTrackingWireKeys.ORDER_BONUSES, it) }
        request.orderDelivery?.let { root.put(PurchaseTrackingWireKeys.ORDER_DELIVERY, it) }
        request.orderDiscount?.let { root.put(PurchaseTrackingWireKeys.ORDER_DISCOUNT, it) }
        request.channel?.takeIf { it.isNotBlank() }?.let {
            root.put(PurchaseTrackingWireKeys.CHANNEL, it)
        }

        if (effectiveCustom.isNotEmpty()) {
            val customJson = JSONObject()
            for ((key, value) in effectiveCustom) {
                TrackCustomEventPayloadHelper.putJsonValue(customJson, key, value)
            }
            root.put(PurchaseTrackingWireKeys.CUSTOM, customJson)
        }

        request.recommendedSource?.let { root.put(PurchaseTrackingWireKeys.RECOMMENDED_SOURCE, it) }

        request.stream?.takeIf { it.isNotBlank() }?.let {
            root.put(UserBasicParams.STREAM, it)
        }
        request.segment?.takeIf { it.isNotBlank() }?.let {
            root.put(UserBasicParams.SEGMENT, it)
        }

        if (request.recommendedBy != null) {
            val rbJson = Params().put(request.recommendedBy).build()
            mergeInto(root, rbJson)
        }

        return root
    }

    private fun itemToJson(item: PurchaseItemRequest): JSONObject {
        val row = JSONObject()
        row.put(PurchaseTrackingWireKeys.ID, item.id)
        row.put(PurchaseTrackingWireKeys.AMOUNT, item.amount)
        row.put(PurchaseTrackingWireKeys.PRICE, item.price)
        item.quantity?.let { row.put(PurchaseTrackingWireKeys.QUANTITY, it) }
        item.lineId?.takeIf { it.isNotBlank() }?.let { row.put(PurchaseTrackingWireKeys.LINE_ID, it) }
        item.fashionSize?.takeIf { it.isNotBlank() }?.let {
            row.put(PurchaseTrackingWireKeys.FASHION_SIZE, it)
        }
        return row
    }

    fun mergeInto(target: JSONObject, source: JSONObject) {
        val keys = source.keys() ?: return
        while (keys.hasNext()) {
            val key = keys.next()
            target.put(key, source.get(key))
        }
    }
}
