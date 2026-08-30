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

/**
 * The tool an event is attributed to (`recommended_by` on the wire).
 *
 * Mirrors iOS `TrackingSourceType` case for case, so the same source means the same thing on both
 * platforms. Kept separate from the released [Params.RecommendedBy.TYPE], which is part of the public
 * `PurchaseTrackingRequest` surface: adding a constant there would break any host with an exhaustive
 * `when` over it. The wire value travels as a raw string instead — see `Params.putRawRecommendedBy`.
 */
enum class TrackingSourceType(val value: String) {
    DYNAMIC("dynamic"),
    CHAIN("chain"),
    BULK("bulk"),
    TRANSACTIONAL("transactional"),
    INSTANT_SEARCH("instant_search"),
    FULL_SEARCH("full_search"),
    STORIES("stories"),
    WEB_PUSH_DIGEST("web_push_digest"),
    ;

    internal fun toDomainType(): RecommendedBy.TYPE = when (this) {
        DYNAMIC -> RecommendedBy.TYPE.RECOMMENDATION
        CHAIN -> RecommendedBy.TYPE.TRIGGER
        BULK -> RecommendedBy.TYPE.BULK
        TRANSACTIONAL -> RecommendedBy.TYPE.TRANSACTIONAL
        INSTANT_SEARCH -> RecommendedBy.TYPE.INSTANT_SEARCH
        FULL_SEARCH -> RecommendedBy.TYPE.FULL_SEARCH
        STORIES -> RecommendedBy.TYPE.STORIES
        WEB_PUSH_DIGEST -> RecommendedBy.TYPE.WEB_PUSH_DIGEST
    }
}

/** Adds this source to [params] as `recommended_by` plus its code field. */
internal fun Params.putSource(source: TrackingSource): Params =
    putRawRecommendedBy(source.type.value, source.code)
