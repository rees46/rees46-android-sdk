package com.personalizatio.features.track_event

import com.personalizatio.Params
import com.personalizatio.Params.InternalParameter
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
        params.put(InternalParameter.EVENT, event.value)
        if (sdk.lastRecommendedBy != null) {
            params.put(sdk.lastRecommendedBy!!)
            sdk.lastRecommendedBy = null
        }
        sdk.sendAsync(PUSH_REQUEST, params.build(), listener)
    }

    override fun track(
        event: String,
        category: String?,
        label: String?,
        value: Int?,
        listener: OnApiCallbackListener?
    ) {
        val params = Params()
        params.put(InternalParameter.EVENT, event)
        if (category != null) {
            params.put(InternalParameter.CATEGORY, category)
        }
        if (label != null) {
            params.put(InternalParameter.LABEL, label)
        }
        if (value != null) {
            params.put(InternalParameter.VALUE, value)
        }
        sdk.sendAsync(CUSTOM_PUSH_REQUEST, params.build(), listener)
    }

    companion object {
        private const val CUSTOM_PUSH_REQUEST = "push/custom"
        private const val PUSH_REQUEST = "push"
    }
}
