package com.personalization.api.managers

import com.personalization.api.OnApiCallbackListener
import com.personalization.api.models.purchase.PurchaseTrackingRequest
import com.personalization.api.models.tracking.TrackingItem
import com.personalization.api.models.tracking.TrackingSource

/**
 * Standard tracking events — the `tracking` namespace of the SDK.
 *
 * Reached through an SDK instance: `Rees46.getInstance().tracking.productView("sku-1")`.
 */
interface TrackingApi {

    /** Product page opened (`view`). */
    fun productView(
        itemId: String,
        source: TrackingSource? = null,
        listener: OnApiCallbackListener? = null,
    )

    /** Category listing opened (`category`). */
    fun categoryView(
        categoryId: String,
        listener: OnApiCallbackListener? = null,
    )

    /**
     * Search query issued by the user (`search`).
     *
     * Pass [results] when the host runs its own search and knows the ids it showed. They go on the
     * wire as one comma-separated field, so ids must not themselves contain a comma.
     */
    fun search(
        query: String,
        results: List<String>? = null,
        listener: OnApiCallbackListener? = null,
    )

    /** One product added to the cart (`cart`). */
    fun addToCart(
        item: TrackingItem,
        source: TrackingSource? = null,
        listener: OnApiCallbackListener? = null,
    )

    /**
     * Full cart contents after a change (`cart` with `full_cart`).
     *
     * Pass an empty list when the cart was emptied — the request then carries an empty `items` list,
     * which is how the backend learns the cart is gone.
     */
    fun syncCart(
        items: List<TrackingItem>,
        listener: OnApiCallbackListener? = null,
    )

    /** One product removed from the cart (`remove_from_cart`). */
    fun removeFromCart(
        itemId: String,
        listener: OnApiCallbackListener? = null,
    )

    /** One product added to favorites (`wish`). */
    fun addToFavorites(
        itemId: String,
        source: TrackingSource? = null,
        listener: OnApiCallbackListener? = null,
    )

    /** Full favorites contents after a change (`wish` with `full_wish`). */
    fun syncFavorites(
        itemIds: List<String>,
        listener: OnApiCallbackListener? = null,
    )

    /** One product removed from favorites (`remove_wish`). */
    fun removeFromFavorites(
        itemId: String,
        listener: OnApiCallbackListener? = null,
    )

    /**
     * A story slide was shown (`track/stories`, `view`).
     *
     * [code] is the stories block code; when omitted the SDK uses the code of the block it
     * last loaded.
     */
    fun storyView(
        storyId: String,
        slideId: String,
        code: String? = null,
        listener: OnApiCallbackListener? = null,
    )

    /** A story slide was tapped (`track/stories`, `click`). */
    fun storyClick(
        storyId: String,
        slideId: String,
        code: String? = null,
        listener: OnApiCallbackListener? = null,
    )

    /**
     * Completed order (`purchase`).
     *
     * [source] attributes the order; an attribution already set on [request] wins over it.
     */
    fun purchase(
        request: PurchaseTrackingRequest,
        source: TrackingSource? = null,
        listener: OnApiCallbackListener? = null,
    )

    /**
     * Custom event defined by the shop (`push/custom`).
     *
     * [customFields] is the one deliberately free-form field: its entries are sent at the top
     * level and duplicated under `payload`. Reserved keys are rejected.
     */
    fun custom(
        event: String,
        time: Int? = null,
        category: String? = null,
        label: String? = null,
        value: Int? = null,
        customFields: Map<String, Any?>? = null,
        listener: OnApiCallbackListener? = null,
    )

    /**
     * Stores the attribution source and attaches it to the next event.
     *
     * Use it when the source outlives a single call — a user entering the catalog from a recommender
     * block. For a single event prefer the `source` parameter, which behaves identically on every
     * platform; the stored source is per instance, but its lifetime is platform-specific (iOS keeps
     * it for 48 hours and colours every event in that window, Android applies it once).
     */
    fun setSource(source: TrackingSource)
}
