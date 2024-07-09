package com.personalizatio.api.managers

import com.personalizatio.Params
import com.personalizatio.Params.TrackEvent
import com.personalizatio.api.OnApiCallbackListener

interface TrackEventManager {

    /**
     * Event tracking
     *
     * @param event Event type
     * @param productId Product ID
     */
    fun track(event: TrackEvent, productId: String)

    /**
     * Event tracking
     *
     * @param event Event type
     * @param params Parameters for the request
     * @param listener Callback
     */
    fun track(
        event: TrackEvent,
        params: Params,
        listener: OnApiCallbackListener? = null
    )

    /**
     * Tracking custom events
     *
     * @param event Event key
     * @param category Event category
     * @param label Event label
     * @param value Event value
     * @param listener Callback
     */
    fun track(
        event: String,
        category: String? = null,
        label: String? = null,
        value: Int? = null,
        listener: OnApiCallbackListener? = null
    )
}
