package com.personalization.sdk.data.repositories.trackingSource

import com.personalization.sdk.domain.models.StoredTrackingSource

interface TrackingSourceDataSource {

    /** Persists the source and starts a fresh window. */
    fun setSource(source: StoredTrackingSource)

    /** The stored source, or `null` when nothing was set or the window has expired. */
    fun getSource(): StoredTrackingSource?
}
