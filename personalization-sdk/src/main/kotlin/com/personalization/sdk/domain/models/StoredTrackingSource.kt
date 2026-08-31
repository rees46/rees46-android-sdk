package com.personalization.sdk.domain.models

/**
 * The attribution a host set with `tracking.setSource(...)`, kept until it expires and attached to
 * every event sent in the meantime.
 *
 * [type] is the raw wire value (`dynamic`, `stories`, …) rather than an enum: the store has to be
 * able to hold sources the released enums have no constant for. Mirrors iOS `TrackingSourceStore`.
 */
data class StoredTrackingSource(
    val type: String,
    val code: String,
)
