package com.personalization.features.trackEvent.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personalization.Params
import com.personalization.api.models.purchase.PurchaseItemRequest
import com.personalization.api.models.purchase.PurchaseTrackingRequest
import com.personalization.api.models.purchase.PurchaseTrackingWireKeys
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PurchaseTrackingJsonBuilderInstrumentedTest {

    @Test
    fun buildOrError_minimal_containsRequiredKeysAndOmitsOptionals() {
        val request = PurchaseTrackingRequest(
            orderId = "order-1",
            orderPrice = 99.5,
            items = listOf(
                PurchaseItemRequest(id = "p1", amount = 2, price = 10.0),
            ),
        )
        val json = PurchaseTrackingJsonBuilder.buildOrError(request).getOrThrow()
        assertEquals("purchase", json.getString(PurchaseTrackingWireKeys.EVENT))
        assertEquals("order-1", json.getString(PurchaseTrackingWireKeys.ORDER_ID))
        assertEquals(99.5, json.getDouble(PurchaseTrackingWireKeys.ORDER_PRICE), 0.0001)
        assertFalse(json.has(PurchaseTrackingWireKeys.TAX_FREE))
        assertFalse(json.has(PurchaseTrackingWireKeys.GIFT_PACKAGE))
        assertFalse(json.has(PurchaseTrackingWireKeys.CUSTOM))
        val items = json.getJSONArray(PurchaseTrackingWireKeys.ITEMS)
        assertEquals(1, items.length())
        val row = items.getJSONObject(0)
        assertEquals("p1", row.getString(PurchaseTrackingWireKeys.ID))
        assertEquals(2, row.getInt(PurchaseTrackingWireKeys.AMOUNT))
        assertEquals(10.0, row.getDouble(PurchaseTrackingWireKeys.PRICE), 0.0001)
        assertFalse(row.has(PurchaseTrackingWireKeys.QUANTITY))
    }

    @Test
    fun buildOrError_full_includesOptionalFields() {
        val request = PurchaseTrackingRequest(
            orderId = "order-2",
            orderPrice = 200.0,
            items = listOf(
                PurchaseItemRequest(
                    id = "p2",
                    amount = 1,
                    price = 50.0,
                    quantity = 1,
                    lineId = "line-1",
                    fashionSize = "M",
                ),
            ),
            deliveryType = "pickup",
            deliveryAddress = "Street 1",
            paymentType = "cash",
            isTaxFree = true,
            promocode = "PROMO",
            orderCash = 10.0,
            orderBonuses = 5.0,
            orderDelivery = 2.0,
            orderDiscount = 1.0,
            channel = "mobile",
            custom = mapOf("k" to "v"),
            recommendedBy = Params.RecommendedBy(Params.RecommendedBy.TYPE.RECOMMENDATION, "code-1"),
            recommendedSource = JSONObject().put("src", 1),
            stream = "s1",
            segment = "B",
            isGiftPackage = true,
        )
        val json = PurchaseTrackingJsonBuilder.buildOrError(request).getOrThrow()
        assertTrue(json.getBoolean(PurchaseTrackingWireKeys.TAX_FREE))
        assertTrue(json.getBoolean(PurchaseTrackingWireKeys.GIFT_PACKAGE))
        assertEquals("pickup", json.getString(PurchaseTrackingWireKeys.DELIVERY_TYPE))
        assertTrue(json.has(PurchaseTrackingWireKeys.CUSTOM))
        assertTrue(json.has(PurchaseTrackingWireKeys.RECOMMENDED_SOURCE))
        assertEquals("dynamic", json.getString(Params.InternalParameter.RECOMMENDED_BY.value))
        assertEquals("code-1", json.getString(Params.InternalParameter.RECOMMENDED_CODE.value))
    }
}
