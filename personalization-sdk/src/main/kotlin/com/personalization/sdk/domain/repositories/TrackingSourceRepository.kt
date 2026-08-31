package com.personalization.sdk.domain.repositories

import com.personalization.sdk.domain.models.StoredTrackingSource

interface TrackingSourceRepository {

    fun setSource(source: StoredTrackingSource)

    fun getSource(): StoredTrackingSource?
}
