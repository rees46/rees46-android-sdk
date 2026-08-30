package com.personalization.features.tracking.impl

import com.personalization.Params
import com.personalization.Params.TrackEvent
import com.personalization.api.OnApiCallbackListener
import com.personalization.api.managers.TrackEventManager
import com.personalization.api.managers.TrackingApi
import com.personalization.api.models.purchase.PurchaseTrackingRequest
import com.personalization.api.models.tracking.TrackingItem
import com.personalization.api.models.tracking.TrackingSource
import com.personalization.api.models.tracking.putSource
import com.personalization.api.params.ProductItemParams
import com.personalization.sdk.domain.models.RecommendedBy
import com.personalization.sdk.domain.usecases.recommendation.SetRecommendedByUseCase
import com.personalization.stories.StoriesManager
import org.json.JSONArray
import javax.inject.Inject

/**
 * Implementation of the `tracking` namespace.
 *
 * It owns no logic of its own: every call is translated into the parameters the API expects and
 * handed to the same managers the (deprecated) root-level tracking methods use, so behaviour —
 * popup handling, stored-source attribution, request queueing — is unchanged.
 */
internal class TrackingApiImpl @Inject constructor(
    private val trackEventManager: TrackEventManager,
    private val storiesManager: StoriesManager,
    private val setRecommendedByUseCase: SetRecommendedByUseCase,
) : TrackingApi {

    override fun productView(
        itemId: String,
        source: TrackingSource?,
        listener: OnApiCallbackListener?,
    ) {
        trackEventManager.track(
            event = TrackEvent.VIEW,
            params = Params().put(ProductItemParams(itemId)).withSource(source),
            listener = listener,
        )
    }

    override fun categoryView(categoryId: String, listener: OnApiCallbackListener?) {
        trackEventManager.track(
            event = TrackEvent.CATEGORY,
            params = Params().put(Params.Parameter.CATEGORY_ID, categoryId),
            listener = listener,
        )
    }

    override fun search(
        query: String,
        results: List<String>?,
        listener: OnApiCallbackListener?,
    ) {
        val params = Params().put(Params.Parameter.SEARCH_QUERY, query)
        if (!results.isNullOrEmpty()) {
            params.put(Params.RESULTS_PARAM, results.joinToString(separator = ","))
        }
        trackEventManager.track(event = TrackEvent.SEARCH, params = params, listener = listener)
    }

    override fun addToCart(
        item: TrackingItem,
        source: TrackingSource?,
        listener: OnApiCallbackListener?,
    ) {
        trackEventManager.track(
            event = TrackEvent.CART,
            params = Params().put(item.toProductParams()).withSource(source),
            listener = listener,
        )
    }

    override fun syncCart(items: List<TrackingItem>, listener: OnApiCallbackListener?) {
        val params = Params()
        items.forEach { params.put(it.toProductParams()) }
        params.putEmptyItemsIfNone()
        params.put(Params.Parameter.FULL_CART, true)
        trackEventManager.track(event = TrackEvent.CART, params = params, listener = listener)
    }

    override fun removeFromCart(itemId: String, listener: OnApiCallbackListener?) {
        trackEventManager.track(
            event = TrackEvent.REMOVE_FROM_CART,
            params = Params().put(ProductItemParams(itemId)),
            listener = listener,
        )
    }

    override fun addToFavorites(
        itemId: String,
        source: TrackingSource?,
        listener: OnApiCallbackListener?,
    ) {
        trackEventManager.track(
            event = TrackEvent.WISH,
            params = Params().put(ProductItemParams(itemId)).withSource(source),
            listener = listener,
        )
    }

    override fun syncFavorites(itemIds: List<String>, listener: OnApiCallbackListener?) {
        val params = Params()
        itemIds.forEach { params.put(ProductItemParams(it)) }
        params.putEmptyItemsIfNone()
        params.put(Params.Parameter.FULL_WISH, true)
        trackEventManager.track(event = TrackEvent.WISH, params = params, listener = listener)
    }

    override fun removeFromFavorites(itemId: String, listener: OnApiCallbackListener?) {
        trackEventManager.track(
            event = TrackEvent.REMOVE_FROM_WISH,
            params = Params().put(ProductItemParams(itemId)),
            listener = listener,
        )
    }

    override fun storyView(
        storyId: String,
        slideId: String,
        code: String?,
        listener: OnApiCallbackListener?,
    ) {
        storiesManager.trackStory(
            event = STORY_VIEW_EVENT,
            code = code,
            storyId = storyId,
            slideId = slideId,
            listener = listener,
        )
    }

    override fun storyClick(
        storyId: String,
        slideId: String,
        code: String?,
        listener: OnApiCallbackListener?,
    ) {
        storiesManager.trackStory(
            event = STORY_CLICK_EVENT,
            code = code,
            storyId = storyId,
            slideId = slideId,
            listener = listener,
        )
    }

    override fun purchase(
        request: PurchaseTrackingRequest,
        source: TrackingSource?,
        listener: OnApiCallbackListener?,
    ) {
        // An attribution already set on the request is the more specific one — it was built with the
        // order — so [source] only fills the gap when the caller left it empty. It is handed over as
        // the pending source, which `trackPurchase` merges into this one request and then clears:
        // that path speaks raw wire values, so every TrackingSourceType works, including the ones the
        // released `Params.RecommendedBy.TYPE` has no constant for.
        if (source != null && request.recommendedBy == null) {
            setSource(source)
        }
        trackEventManager.trackPurchase(request = request, listener = listener)
    }

    override fun custom(
        event: String,
        time: Int?,
        category: String?,
        label: String?,
        value: Int?,
        customFields: Map<String, Any?>?,
        listener: OnApiCallbackListener?,
    ) {
        trackEventManager.trackEvent(
            event = event,
            time = time,
            category = category,
            label = label,
            value = value,
            customFields = customFields,
            listener = listener,
        )
    }

    override fun setSource(source: TrackingSource) {
        setRecommendedByUseCase(
            RecommendedBy(type = source.type.toDomainType(), code = source.code)
        )
    }

    private fun Params.withSource(source: TrackingSource?): Params =
        source?.let { putSource(it) } ?: this

    /**
     * "The cart is now empty" is a real sync, and it has to say so: without this the request would
     * carry `full_cart` and no `items` at all, which reads as "nothing changed". Matches iOS, which
     * always sends the list.
     */
    private fun Params.putEmptyItemsIfNone() {
        if (!build().has(ITEMS_PARAM)) build().put(ITEMS_PARAM, JSONArray())
    }

    private fun TrackingItem.toProductParams(): ProductItemParams {
        val params = ProductItemParams(id).set(ProductItemParams.PARAMETER.AMOUNT, quantity)
        price?.let { params.set(ProductItemParams.PARAMETER.PRICE, it) }
        fashionSize?.let { params.set(ProductItemParams.PARAMETER.FASHION_SIZE, it) }
        return params
    }

    private companion object {
        const val STORY_VIEW_EVENT = "view"
        const val STORY_CLICK_EVENT = "click"
        const val ITEMS_PARAM = "items"
    }
}
