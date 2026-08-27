package com.personalization.api.models.tracking

import com.personalization.Params
import com.personalization.sdk.domain.models.RecommendedBy

/**
 * One product line in a tracking event.
 *
 * [quantity] is the domain name for the line quantity; on the wire it is sent as `amount` —
 * the field the REES46 API has always consumed.
 */
data class TrackingItem(
    val id: String,
    val quantity: Int = 1,
    val price: Double? = null,
    val fashionSize: String? = null,
)

/** Where a tracked action came from — a recommender block, a search result, a story. */
data class TrackingSource(
    val type: TrackingSourceType,
    val code: String,
)

/** The tool an event is attributed to (`recommended_by` on the wire). */
enum class TrackingSourceType(val value: String) {
    DYNAMIC("dynamic"),
    CHAIN("chain"),
    BULK("bulk"),
    TRANSACTIONAL("transactional"),
    INSTANT_SEARCH("instant_search"),
    FULL_SEARCH("full_search"),
    STORIES("stories"),
    ;

    internal fun toParamsType(): Params.RecommendedBy.TYPE = when (this) {
        DYNAMIC -> Params.RecommendedBy.TYPE.RECOMMENDATION
        CHAIN -> Params.RecommendedBy.TYPE.TRIGGER
        BULK -> Params.RecommendedBy.TYPE.BULK
        TRANSACTIONAL -> Params.RecommendedBy.TYPE.TRANSACTIONAL
        INSTANT_SEARCH -> Params.RecommendedBy.TYPE.INSTANT_SEARCH
        FULL_SEARCH -> Params.RecommendedBy.TYPE.FULL_SEARCH
        STORIES -> Params.RecommendedBy.TYPE.STORIES
    }

    internal fun toDomainType(): RecommendedBy.TYPE = when (this) {
        DYNAMIC -> RecommendedBy.TYPE.RECOMMENDATION
        CHAIN -> RecommendedBy.TYPE.TRIGGER
        BULK -> RecommendedBy.TYPE.BULK
        TRANSACTIONAL -> RecommendedBy.TYPE.TRANSACTIONAL
        INSTANT_SEARCH -> RecommendedBy.TYPE.INSTANT_SEARCH
        FULL_SEARCH -> RecommendedBy.TYPE.FULL_SEARCH
        STORIES -> RecommendedBy.TYPE.STORIES
    }
}

internal fun TrackingSource.toParamsRecommendedBy(): Params.RecommendedBy =
    Params.RecommendedBy(type.toParamsType(), code)
