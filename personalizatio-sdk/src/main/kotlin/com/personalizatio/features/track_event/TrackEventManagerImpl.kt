package com.personalizatio.features.track_event

import com.personalizatio.Params
import com.personalizatio.Params.TrackEvent
import com.personalizatio.SDK
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.managers.TrackEventManager
import com.personalizatio.api.params.ProductItemParams

internal class TrackEventManagerImpl(val sdk: SDK) : TrackEventManager {

    override fun track(event: TrackEvent, productId: String) {
        track(event, Params().put(ProductItemParams(productId)), null)
    }

    override fun track(
        event: TrackEvent,
        params: Params,
        listener: OnApiCallbackListener?
    ) {
        params.put(EVENT_PARAMETER, event.value)
        if (sdk.lastRecommendedBy != null) {
            params.put(sdk.lastRecommendedBy!!)
            sdk.lastRecommendedBy = null
        }
        sdk.sendAsync(PUSH_REQUEST, params.build(), listener)
    }

    override fun customTrack(
        event: String,
        email: String?,
        phone: String?,
        loyaltyId: String?,
        externalId: String?,
        category: String?,
        label: String?,
        value: Int?,
        listener: OnApiCallbackListener?
    ) {
        val params = Params()
        params.put(EVENT_PARAMETER, event)
        if (email != null) { params.put(EMAIL_PARAMETER, email) }
        if (phone != null) { params.put(PHONE_PARAMETER, phone) }
        if (loyaltyId != null) { params.put(LOYALTY_ID_PARAMETER, loyaltyId) }
        if (externalId != null) { params.put(EXTERNAL_ID_PARAMETER, externalId) }
        if (category != null) { params.put(CATEGORY_PARAMETER, category) }
        if (label != null) { params.put(LABEL_PARAMETER, label) }
        if (value != null) { params.put(VALUE_PARAMETER, value) }

        sdk.sendAsync(CUSTOM_PUSH_REQUEST, params.build(), listener)
    }

    companion object {
        private const val CUSTOM_PUSH_REQUEST = "push/custom"
        private const val PUSH_REQUEST = "push"

        private const val EVENT_PARAMETER = "event"
        private const val EMAIL_PARAMETER = "email"
        private const val PHONE_PARAMETER = "phone"
        private const val LOYALTY_ID_PARAMETER = "loyalty_id"
        private const val EXTERNAL_ID_PARAMETER = "external_id"
        private const val CATEGORY_PARAMETER = "category"
        private const val LABEL_PARAMETER = "label"
        private const val VALUE_PARAMETER = "value"
    }
}